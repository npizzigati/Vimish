package org.omegat.plugins.vimish;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.omegat.util.Log;

class KeySequence {
  private String sequence = "";
  private Actions actions;
  private int actionsCount = 0;
  private Configuration configuration;
  // User-entered sequence is checked against these sequences
  // *after* all possible mode action matches are checked (if none
  // matches), to see whether sequence is valid but incomplete
  // (and may constitute a match if user enters additional character(s))

  private Pattern[] validPartialNormalSequences = {Pattern.compile("^\\d*[dcyxPp]?\\d*[hlwWeEbBfFtTnNrR]?"),
                                                   Pattern.compile("^[dcy]?[ai]?"),
                                                   Pattern.compile("^[dcy]"),
                                                   Pattern.compile("^\"")};
  private Pattern[] validPartialVisualSequences = {Pattern.compile("^[fFtTrR]"),
                                                   Pattern.compile("^\\d*[rNn]?"),
                                                   Pattern.compile("^[ai]"),
                                                   Pattern.compile("^\"")};

  KeySequence(Actions actions) {
    this.actions = actions;
    configuration = Configuration.getConfiguration();
  }

  boolean isInProgress() {
    Log.log("actionsCount: " + actionsCount);
    return actionsCount > 0;
  }

  boolean isValid(String keyString) {
    boolean isPatternValid = false;
    Pattern[] patterns = {};
    if (Mode.NORMAL.isActive()) {
      patterns = validPartialNormalSequences;
    } else if (Mode.VISUAL.isActive()) {
      patterns = validPartialVisualSequences;
    }
    for (Pattern pattern : patterns) {
      Matcher m = pattern.matcher(sequence);
      if (m.find() && m.group().equals(sequence)) {
        Log.log("Sequence is valid");
        isPatternValid = true;
        break;
      }
    }
    return isPatternValid;
  }

  void apply(String keyString) {
    sequence += keyString;
    Log.log("sequence: " + sequence);

    // Test sequence with each new keyString addition, so see if
    // it could still potentially match one of the action sequences.
    // If not, abort and return
    actionsCount += 1;
    String newSequence = sequence;
    if (Mode.NORMAL.isActive()) {
      newSequence = evaluateNormalSequence();
    } else if (Mode.INSERT.isActive()) {
      newSequence = evaluateInsertSequence();
    } else if (Mode.VISUAL.isActive()) {
      newSequence = evaluateVisualSequence();
    } else if (Mode.REPLACE.isActive()) {
      newSequence = evaluateReplaceSequence();
    } else if (Mode.SEARCH.isActive()) {
      newSequence = evaluateSearchSequence();
    }

    boolean didSequenceChange = !sequence.equals(newSequence);
    // If sequence didn't change, this means no portion was
    // matched. Check to see if a future match will be possible
    // if the user enters more text by checking whether the
    // sequence is valid.
    if (!didSequenceChange) {
      if (!isValid(keyString)) {
        Log.log("Invalid sequence");
        sequence = "";
        actionsCount = 0;
        return;
      }
    }
    sequence = newSequence;

    // Recursively call apply on rest of sequence with no
    // additional keyString, if sequence is not empty and it has
    // changed -- i.e., some portion of the sequence has
    // triggered a match in the evaluate... methods above and the
    // sequence is now the remainder of the previous sequence.
    // Count number of actions applied to prevent runaway
    if (sequence.equals("") || actionsCount > 25) {
      actionsCount = 0;
      return;
    }
    if (didSequenceChange) {
      apply("");
    }
  }

  private String evaluateVisualSequence() {
    String newSequence = sequence;
    /*
     * Handle to/till (f/t) and forward and backward search. These two regex
     * sections must come first, since the following sections assume that they have
     * already been processed.
     *
     * The order of subsequent regexes shouldn't matter
     **/
    // To or till
    if (sequence.matches("^\\d*[fFTt]..*")) {
      Matcher matcher = Pattern.compile("^(\\d*)([fFTt])(.)(.*)").matcher(sequence);
      matcher.find();
      String countString = matcher.group(1);
      String motion = matcher.group(2);
      String key = matcher.group(3);
      String remainder = matcher.group(4);

      int count = (countString.equals("")) ? 1 : Integer.parseInt(countString, 10);
      if (motion.equals("f") || motion.equals("t")) {
        actions.visualModeGoForwardToChar(count, motion, key);
      } else {
        actions.visualModeGoBackwardToChar(count, motion, key);
      }
      newSequence = remainder;
    }

    else if (sequence.matches("^[?/].*")) {
      Matcher matcher = Pattern.compile("^([?/])(.*)")
                             .matcher(sequence);
      matcher.find();
      String operator = matcher.group(1);
      String remainder = matcher.group(2);
      Mode.SEARCH.activate();
      actions.normalModeActivateSearch(operator, Mode.VISUAL);
      newSequence = remainder;
    }

    // u/U - upcase/downcase
    else if (sequence.matches("^[uU].*")) {
      Matcher matcher = Pattern.compile("^([uU])(.*)").matcher(sequence);
      matcher.find();
      String operator = matcher.group(1);
      String remainder = matcher.group(2);
      actions.visualModeSwitchCase(operator);
      newSequence = remainder;
    }

    // r - little "r" replace
    else if (sequence.matches("^\\d*r..*")) {
      // Numbers before r will be ignored
      Matcher matcher = Pattern.compile("^\\d*r(.)(.*)").matcher(sequence);
      matcher.find();
      String key = matcher.group(1);
      String remainder = matcher.group(2);
      actions.visualModeReplace(key);
      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      newSequence = remainder;
    }

    else if (sequence.matches("[DCY].*")) {
      Matcher matcher = Pattern.compile("([DCY])(.*)")
                             .matcher(sequence);
      matcher.find();
      String operator = matcher.group(1);
      String remainder = matcher.group(2);
      actions.visualModeBigDCY(operator);
      newSequence = remainder;
    }

    else if (sequence.matches("[$0].*")) {
      Matcher matcher = Pattern.compile("([$0])(.*)")
                             .matcher(sequence);
      matcher.find();
      String motion = matcher.group(1);
      String remainder = matcher.group(2);
      actions.visualModeGoToSegmentBoundary(motion);
      newSequence = remainder;
    }


    // This regex does not account for the fact that an escape
    // will not always take you to normal mode (e.g.
    // it can also escape from another operation, like in the case of
    // a<ESC> or i<ESC>
    else if (sequence.matches("^<ESC>.*")) {
      Matcher matcher = Pattern.compile("^<ESC>(.*)").matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);

      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      newSequence = remainder;
    }

    else if (sequence.matches("^\\d*v.*")) {
      // What other key combinations should make us escape back
      // to normal mode?
      // I can also combine these cases in a single if statement
      Matcher matcher = Pattern.compile("^\\d*v(.*)").matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);

      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      newSequence = remainder;
    }

    else if (sequence.matches("^[dx].*")) {
      Matcher matcher = Pattern.compile("^[dx](.*)").matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);

      actions.visualModeDelete();
      actions.clearVisualMarks();
      Mode.NORMAL.activate();
      newSequence = remainder;
    } else if (sequence.matches("^c.*")) {
      Matcher matcher = Pattern.compile("^c(.*)").matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);

      actions.visualModeDelete();
      actions.clearVisualMarks();
      Mode.INSERT.activate();
      newSequence = remainder;
    } else if (sequence.matches("^y.*")) {
      Matcher matcher = Pattern.compile("^y(.*)").matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);

      actions.visualModeYank();
      actions.clearVisualMarks();
      Mode.NORMAL.activate();
      newSequence = remainder;
    }

    else if (sequence.matches("^\\d*[nN].*")) {
      Matcher matcher = Pattern.compile("(^\\d*)([nN])(.*)").matcher(sequence);
      matcher.find();
      String countString = matcher.group(1);
      String motion = matcher.group(2);
      String remainder = matcher.group(3);
      int count = (countString.equals("")) ? 1 : Integer.parseInt(countString, 10);
      if (motion.equals("n")) {
        actions.repeatForwardSearch(count, "");
      } else {
        actions.repeatBackwardSearch(count, "");
      }
      newSequence = remainder;
    }

    else if (sequence.matches("^\\d*[hlwWeEbB].*")) {
      // Handle h/l motions (character left and right)
      Matcher matcher = Pattern.compile("^(\\d*)([hlwWeEbB])(.*)").matcher(sequence);
      matcher.find();
      String countString = matcher.group(1);
      String motion = matcher.group(2);
      String remainder = matcher.group(3);

      int count = (countString.equals("")) ? 1 : Integer.parseInt(countString, 10);

      if (motion.equals("h")) {
        actions.visualModeBackwardChar(count);
      } else if (motion.equals("l")) {
        actions.visualModeForwardChar(count);
      } else if (motion.toLowerCase().equals("w") || motion.toLowerCase().equals("e")) {
        actions.visualModeForwardWord(motion, count);
      } else if (motion.toLowerCase().equals("b")) {
        actions.visualModeBackwardWord(motion, count);
      }
      newSequence = remainder;

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments
    }

    // Text object selection
    else if (sequence.matches("^[ai][wW\"\'\\[\\]\\{\\}<>\\(\\)].*")) {
      Matcher matcher = Pattern.compile("^([ia])([wW\"\'\\[\\]\\{\\}<>\\(\\)])(.*)")
                             .matcher(sequence);
      matcher.find();
      String selector = matcher.group(1);
      String object = matcher.group(2);
      String remainder = matcher.group(3);
      actions.visualModeTextObjectSelection(selector, object);
      newSequence = remainder;
    } else if (sequence.matches(".*<ESC><ESC>")) {
      newSequence = "";
    }

    return newSequence;
  }

  private String evaluateReplaceSequence() {
    String newSequence = sequence;
    if (sequence.matches("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>).*")) {
      Matcher matcher = Pattern.compile("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>)(.*)").matcher(sequence);
      matcher.find();
      String key = matcher.group(1);
      String remainder = matcher.group(2);
      switch (key) {
        case "<ESC>":
          Mode.NORMAL.activate();
          if (configuration.getConfigMoveCursorBack() || actions.isCaretPastLastIndex()) {
            actions.normalModeBackwardChar(1);
          }
          break;
        case "<BACKSPACE>":
          actions.replaceModeBackspace();
          Log.log("Backspace evaluated");
          break;
        case "<TAB>":
          actions.replaceModeTab();
          Log.log("Tab evaluated");
          break;
        case "<S-TAB>":
          actions.replaceModeShiftTab();
          Log.log("Shift-Tab evaluated");
          break;
        case "<ENTER>":
          actions.replaceModeEnter();
          Log.log("Enter evaluated");
          break;
        case "<DEL>":
          actions.replaceModeDelete();
          Log.log("Delete evaluated");
          break;
      }
      newSequence = remainder;
    } else {
      String character = sequence.substring(0, 1);
      String remainder = sequence.substring(1);
      actions.replaceModeInsertText(character);
      newSequence = remainder;
    }

    return newSequence;
  }

  private String evaluateInsertSequence() {
    String newSequence = sequence;
    if (sequence.matches("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>).*")) {
      Matcher matcher = Pattern.compile("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>)(.*)").matcher(sequence);
      matcher.find();
      String key = matcher.group(1);
      String remainder = matcher.group(2);
      switch (key) {
        case "<ESC>":
          Mode.NORMAL.activate();
          if (configuration.getConfigMoveCursorBack() || actions.isCaretPastLastIndex()) {
            actions.normalModeBackwardChar(1);
          }
          break;
        case "<BACKSPACE>":
          actions.insertModeBackspace();
          Log.log("Backspace evaluated");
          break;
        case "<TAB>":
          actions.insertModeTab();
          Log.log("Tab evaluated");
          break;
        case "<S-TAB>":
          actions.insertModeShiftTab();
          Log.log("Shift-Tab evaluated");
          break;
        case "<ENTER>":
          actions.insertModeEnter();
          Log.log("Enter evaluated");
          break;
        case "<DEL>":
          actions.insertModeDelete();
          Log.log("Delete evaluated");
          break;
      }
      newSequence = remainder;
    } else {
      String character = sequence.substring(0, 1);
      String remainder = sequence.substring(1);
      actions.insertModeInsertText(character);
      newSequence = remainder;
    }

    return newSequence;
  }

  // private String removeEvaluatedPart(String sequence, String entireMatchString)
  // {
  // return sequence.replaceFirst(entireMatchString, "");
  // }

  private String evaluateSearchSequence() {
    String newSequence = sequence;
    if (sequence.matches("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>).*")) {
      Matcher matcher = Pattern.compile("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>)(.*)").matcher(sequence);
      matcher.find();
      String key = matcher.group(1);
      String remainder = matcher.group(2);
      switch (key) {
        case "<BACKSPACE>":
          actions.searchModeBackspace();
          break;
        case "<ENTER>":
          actions.searchModeExecuteSearch();
          break;
        case "<TAB>":
        case "<S-TAB>":
        case "<DEL>":
        case "<ESC>":
          actions.searchModeFinalizeSearch(true);
          break;
      }
      newSequence = remainder;
    } else {
      String character = sequence.substring(0, 1);
      String remainder = sequence.substring(1);
      actions.searchModeAddChar(character);
      newSequence = remainder;
    }
    return newSequence;
  }

  private String evaluateNormalSequence() {
    String newSequence = sequence;
    /*
     * Handle to/till (f/t) and forward and backward search. These two regex
     * sections must come first, since the following sections assume that they have
     * already been processed.
     *
     * The order of subsequent regexes shouldn't matter
     **/
    // To or till or search
    if (sequence.matches("^\\d*[dcy]?\\d*[fFTt]..*")) {
      Matcher matcher = Pattern.compile("^(\\d*)([dcy]?)(\\d*)([fFTt])(.)(.*)").matcher(sequence);
      matcher.find();
      String countString1 = matcher.group(1);
      String operator = matcher.group(2);
      String countString2 = matcher.group(3);
      String motion = matcher.group(4);
      String key = matcher.group(5);
      String remainder = matcher.group(6);

      int count = determineTotalCount(countString1, countString2);
      if (motion.equals("f") || motion.equals("t")) {
        actions.normalModeGoForwardToChar(count, operator, motion, key);
      } else {
        actions.normalModeGoBackwardToChar(count, operator, motion, key);
      }
      newSequence = remainder;
    }

    else if (sequence.matches("^[?/].*")) {
      Matcher matcher = Pattern.compile("^([?/])(.*)")
                             .matcher(sequence);
      matcher.find();
      String operator = matcher.group(1);
      String remainder = matcher.group(2);
      Mode.SEARCH.activate();
      actions.normalModeActivateSearch(operator, Mode.NORMAL);
      newSequence = remainder;
    }

    // r - little "r" replace
    else if (sequence.matches("^\\d*r..*")) {
      Matcher matcher = Pattern.compile("(^\\d*)r(.)(.*)").matcher(sequence);
      matcher.find();
      String countString = matcher.group(1);
      String key = matcher.group(2);
      String remainder = matcher.group(3);
      int count = (countString.equals("")) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeReplace(key, count);
      newSequence = remainder;
    }

    // R - big "R" replace
    else if (sequence.matches("^R.*")) {
      Matcher matcher = Pattern.compile("^R(.*)").matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);
      Mode.REPLACE.activate();
      newSequence = remainder;
    }

    // Text object selection (e.g. "diw" -> delete in word;
    // "di)" -> delete in parentheses)
    else if (sequence.matches("^[dcy][ia][wW\"\'\\[\\]\\{\\}<>\\(\\)].*")) {
      Matcher matcher = Pattern.compile("^([dcy])([ia])([wW\"\'\\[\\]\\{\\}<>\\(\\)])(.*)")
                             .matcher(sequence);
      matcher.find();
      String operator = matcher.group(1);
      String selector = matcher.group(2);
      String object = matcher.group(3);
      String remainder = matcher.group(4);
      actions.normalModeTextObjectSelection(operator, selector, object);
      newSequence = remainder;
    }

    else if (sequence.matches("[DCY].*")) {
      Matcher matcher = Pattern.compile("([DCY])(.*)")
                             .matcher(sequence);
      matcher.find();
      String operator = matcher.group(1);
      String remainder = matcher.group(2);
      actions.normalModeBigDCY(operator);
      newSequence = remainder;
    }

    else if (sequence.matches("[$0].*")) {
      Matcher matcher = Pattern.compile("([$0])(.*)")
                             .matcher(sequence);
      matcher.find();
      String motion = matcher.group(1);
      String remainder = matcher.group(2);
      actions.normalModeGoToSegmentBoundary(motion);
      newSequence = remainder;
    }

    else if (sequence.matches("[dcy][$0].*")) {
      Matcher matcher = Pattern.compile("([dcy])([$0])(.*)")
                             .matcher(sequence);
      matcher.find();
      String operator = matcher.group(1);
      String motion = matcher.group(2);
      String remainder = matcher.group(3);
      actions.normalModeOperateToSegmentBoundary(operator, motion);
      newSequence = remainder;
    }

    else if (sequence.matches("^x.*")) {
      // Need to fill this in
      Matcher matcher = Pattern.compile("^x(.*)")
                             .matcher(sequence);
      matcher.find();

      actions.normalModeForwardChar("d", 1);
      String remainder = matcher.group(1);
      newSequence = remainder;
    }

    else if (sequence.matches("^i.*")) {
      Matcher matcher = Pattern.compile("^i(.*)")
                             .matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);

      Mode.INSERT.activate();
      newSequence = remainder;
    }

    else if (sequence.matches("^a.*")) {
      Matcher matcher = Pattern.compile("^a(.*)")
                             .matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);

      actions.normalModeAppendAfterCursor();
      Mode.INSERT.activate();
      newSequence = remainder;
    }

    else if (sequence.matches("^\\d*~.*")) {
      Matcher matcher = Pattern.compile("^(\\d*)~(.*)")
                             .matcher(sequence);
      matcher.find();
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("")) ? 1 : Integer.parseInt(countString, 10);

      actions.normalModeToggleCase(count);
      newSequence = remainder;
    }

    else if (sequence.matches("^u.*")) {
      Matcher matcher = Pattern.compile("^u(.*)")
                             .matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);

      actions.undo();
      newSequence = remainder;
    }

    else if (sequence.matches("^\\d*v.*")) {
      Matcher matcher = Pattern.compile("^(\\d*)v(.*)")
                             .matcher(sequence);
      matcher.find();
      String entireMatch = matcher.group(0);
      Log.log("entireMatch: " + entireMatch);
      String countString = matcher.group(1);
      Log.log("countString: " + countString);
      int count = (countString.equals("")) ? 0 : Integer.parseInt(countString, 10);
      String remainder = matcher.group(2);

      Mode.VISUAL.activate();
      actions.beginSingleCharVisualSelection();
      if (count > 0) {
        actions.visualModeForwardChar(count - 1);
      }
      newSequence = remainder;
    // } else if (sequence.matches("^.*v")) {
    //   // If just a "v" is entered with no preceding number,
    //   // change to visual mode without resetting sequence, so as
    //   // to mark character that caret is on (count is implicitly 1)
    //   Mode.VISUAL.activate();
    //   evaluateVisualSequence("1v");
    }

    /*
     * Put
     **/
    else if (sequence.matches("^\"[0-9a-zA-Z-][pP].*")) {
      Matcher matcher = Pattern.compile("^\"([0-9a-zA-Z-])([pP])(.*)")
                             .matcher(sequence);
      Log.log("Named register put");
      matcher.find();
      String registerKey = matcher.group(1);
      String putLetter = matcher.group(2);
      String remainder = matcher.group(3);
      String position = (putLetter.equals("p")) ? "after" : "before";

      actions.normalModePutSpecificRegister(registerKey, position);
      newSequence = remainder;
    }

    else if (sequence.matches("^([pP]|\"\"[pP]).*")) {

      Matcher matcher = Pattern.compile("^(([pP])|\"\"([pP]))(.*)")
                             .matcher(sequence);
      matcher.find();
      String putLetter = (matcher.group(2).equals("")) ? matcher.group(3) : matcher.group(2);
      String remainder = matcher.group(4);
      String position = (putLetter.equals("p")) ? "after" : "before";

      actions.normalModePutUnnamedRegister(position);
      newSequence = remainder;
    }

    // Repeat search
    else if (sequence.matches("^\\d*[dcy]?\\d*[nN].*")) {
      Matcher matcher = Pattern.compile("^(\\d*)([dcy]?)(\\d*)([nN])(.*)").matcher(sequence);
      matcher.find();
      String countString1 = matcher.group(1);
      String operator = matcher.group(2);
      String countString2 = matcher.group(3);
      String motion = matcher.group(4);
      String remainder = matcher.group(5);
      int totalCount = determineTotalCount(countString1, countString2);
      if (motion.equals("n")) {
        actions.repeatForwardSearch(totalCount, operator);
      } else {
        actions.repeatBackwardSearch(totalCount, operator);
      }
      newSequence = remainder;
    }

    else if (sequence.matches("^\\d*[dcy]?\\d*[hlwWeEbB].*")) {

      // Handle h/l/wW/eE motions (character left and right)
      // with no operator or with d/c/y operators
      Matcher matcher = Pattern.compile("^(\\d*)([dcy]?)(\\d*)([hlwWeEbB])(.*)")
                             .matcher(sequence);
      matcher.find();
      String entireMatch = matcher.group(0);
      Log.log("entireMatch: " + entireMatch);
      String countString1 = matcher.group(1);
      String operator = matcher.group(2);
      String countString2 = matcher.group(3);
      String motion = matcher.group(4);
      String remainder = matcher.group(5);

      int totalCount = determineTotalCount(countString1, countString2);

      if (motion.equals("h")) {
        Log.log("sending back to action with operator" + operator);
        actions.normalModeBackwardChar(operator, totalCount);
      } else if (motion.equals("l")) {
        actions.normalModeForwardChar(operator, totalCount);
      } else if (motion.toLowerCase().equals("w") || motion.toLowerCase().equals("e")) {
        actions.normalModeForwardWord(operator, motion, totalCount);
      } else if (motion.toLowerCase().equals("b")) {
        actions.normalModeBackwardWord(operator, motion, totalCount);
      }
      newSequence = remainder;

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments

      // return sequence.replaceFirst(entireMatchString, "");
    } else if (sequence.matches("^<TAB>.*")) {
      Matcher matcher = Pattern.compile("^<TAB>(.*)")
                             .matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);
      newSequence = remainder;
      actions.normalModeTab();
    } else if (sequence.matches("^<S-TAB>.*")) {
      Matcher matcher = Pattern.compile("^<S-TAB>(.*)")
                             .matcher(sequence);
      matcher.find();
      String remainder = matcher.group(1);
      newSequence = remainder;
      actions.normalModeShiftTab();
      // TODO: Need to fix up the cases below
    } else if (sequence.matches(".*<ESC><ESC>")) {
      newSequence = "";
    }
    // TODO: also need to account for ";" and "," (single char)
    return newSequence;
  }

  int determineTotalCount(String countString1, String countString2) {
    int totalCount = 1;
    int count1 = (countString1.equals("") || countString1.equals("1")) ? 0 : Integer.parseInt(countString1, 10);
    int count2 = (countString2.equals("") || countString2.equals("1")) ? 0 : Integer.parseInt(countString2, 10);

    if (count1 + count2 == 0) {
      totalCount = 1;
    } else {
      totalCount = count1 + count2;
    }
    return totalCount;
  }
}
