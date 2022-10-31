package org.omegat.plugins.vimish;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.omegat.util.Log;

import java.util.ArrayList;
import java.util.List;

class KeySequence {
  private String sequence = "";
  private Actions actions;
  private int actionsCount = 0;
  private Configuration configuration;
  private List<Matcher> visualMatchers = new ArrayList<>();
  private List<Matcher> normalMatchers = new ArrayList<>();

  KeySequence(Actions actions) {
    this.actions = actions;
    configuration = Configuration.getConfiguration();
  }

  boolean isInProgress() {
    return actionsCount > 0;
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
      normalMatchers.clear();
    } else if (Mode.VISUAL.isActive()) {
      newSequence = evaluateVisualSequence();
      visualMatchers.clear();
    } else if (Mode.INSERT.isActive()) {
      newSequence = evaluateInsertSequence();
    } else if (Mode.REPLACE.isActive()) {
      newSequence = evaluateReplaceSequence();
    } else if (Mode.SEARCH.isActive()) {
      newSequence = evaluateSearchSequence();
    }

    boolean didSequenceChange = !sequence.equals(newSequence);

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

  private Matcher getVisualMatcher(String regexp, String sequence) {
    Matcher matcher = Pattern.compile(regexp).matcher(sequence);
    visualMatchers.add(matcher);
    return matcher;
  }

  private Matcher getNormalMatcher(String regexp, String sequence) {
    Matcher matcher = Pattern.compile(regexp).matcher(sequence);
    normalMatchers.add(matcher);
    return matcher;
  }

  private String evaluateNormalSequence() {
    Matcher matcher;
    /*
     * Handle to/till (f/t) and forward and backward search. These two regex
     * sections must come first, since the following sections assume that they have
     * already been processed.
     *
     * The order of subsequent regexes shouldn't matter
     **/
    // To or till or search
    matcher = getNormalMatcher("^(\"([0-9a-zA-Z\\-\"]))?(\\d*)([dcy]?)(\\d*)([fFTt])(.)(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString1 = matcher.group(3);
      String operator = matcher.group(4);
      String countString2 = matcher.group(5);
      String motion = matcher.group(6);
      String key = matcher.group(7);
      String remainder = matcher.group(8);
      int count = determineTotalCount(countString1, countString2);
      if (motion.equals("f") || motion.equals("t")) {
        actions.normalModeGoForwardToChar(count, operator, motion, key, registerKey);
      } else {
        actions.normalModeGoBackwardToChar(count, operator, motion, key, registerKey);
      }
      return remainder;
    }

    matcher = getNormalMatcher("^([?/])(.*)", sequence);
    if (matcher.find()) {
      String operator = matcher.group(1);
      String remainder = matcher.group(2);
      Mode.SEARCH.activate();
      actions.normalModeActivateSearch(operator, Mode.NORMAL);
      return remainder;
    }

    // r - little "r" replace
    matcher = getNormalMatcher("^(\\d*)r(.)(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String key = matcher.group(2);
      String remainder = matcher.group(3);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeReplace(key, count);
      return remainder;
    }

    // R - big "R" replace
    matcher = getNormalMatcher("^R(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      Mode.REPLACE.activate();
      return remainder;
    }

    // Text object selection (e.g. "diw" -> delete in word;
    // "di)" -> delete in parentheses)
    matcher = getNormalMatcher("^(\"([0-9a-zA-Z\\-\"]))?([dcy])([ia])([wW\"\'\\[\\]\\{\\}<>\\(\\)])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String selector = matcher.group(4);
      String object = matcher.group(5);
      String remainder = matcher.group(6);
      actions.normalModeTextObjectSelection(operator, selector, object, registerKey);
      return remainder;
    }

    matcher = getNormalMatcher("^(\"([0-9a-zA-Z\\-\"]))?([DCY])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String remainder = matcher.group(4);
      actions.normalModeBigDCY(operator, registerKey);
      return remainder;
    }

    matcher = getNormalMatcher("^([$0])(.*)", sequence);
    if (matcher.find()) {
      String motion = matcher.group(1);
      String remainder = matcher.group(2);
      actions.normalModeGoToSegmentBoundary(motion);
      return remainder;
    }

    matcher = getNormalMatcher("^(\"([0-9a-zA-Z\\-\"]))?([dcy])([$0])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String motion = matcher.group(4);
      String remainder = matcher.group(5);
      actions.normalModeOperateToSegmentBoundary(operator, motion, registerKey);
      return remainder;
    }

    matcher = getNormalMatcher("^(\"([0-9a-zA-Z\\-\"]))?(\\d*)x(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString = matcher.group(3);
      String remainder = matcher.group(4);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeForwardChar("d", count, registerKey);
      return remainder;
    }

    matcher = getNormalMatcher("^i(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      Mode.INSERT.activate();
      return remainder;
    }

    matcher = getNormalMatcher("^a(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.normalModeAppendAfterCursor();
      Mode.INSERT.activate();
      return remainder;
    }

    matcher = getNormalMatcher("^(\\d*)~(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeToggleCase(count);
      return remainder;
    }

    matcher = getNormalMatcher("^u(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.undo();
      return remainder;
    }

    matcher = getNormalMatcher("^(\\d*)v(.*)", sequence);
    if (matcher.find()) {
      String entireMatch = matcher.group(0);
      Log.log("entireMatch: " + entireMatch);
      String countString = matcher.group(1);
      Log.log("countString: " + countString);
      int count = (countString.equals("") || countString == null) ? 0 : Integer.parseInt(countString, 10);
      String remainder = matcher.group(2);
      Mode.VISUAL.activate();
      actions.beginSingleCharVisualSelection();
      if (count > 0) {
        actions.visualModeForwardChar(count - 1);
      }
      return remainder;
    }

    // Put
    matcher = getNormalMatcher("^(\\d*)(\"([0-9a-zA-Z\\-\"]))?(\\d*)([pP])(.*)", sequence);
    if (matcher.find()) {
      String countString1 = matcher.group(1);
      String registerKey = matcher.group(3);
      String countString2 = matcher.group(4);
      String operator = matcher.group(5);
      String remainder = matcher.group(6);

      int totalCount = determineTotalCount(countString1, countString2);
      actions.normalModePut(registerKey, operator, totalCount);
      return remainder;
    }

    // Repeat search
    matcher = getNormalMatcher("^(\"([0-9a-zA-Z\\-\"]))?(\\d*)([dcy]?)(\\d*)([nN])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString1 = matcher.group(3);
      String operator = matcher.group(4);
      String countString2 = matcher.group(5);
      String motion = matcher.group(6);
      String remainder = matcher.group(7);
      int totalCount = determineTotalCount(countString1, countString2);
      if (motion.equals("n")) {
        actions.repeatForwardSearch(totalCount, operator, registerKey);
      } else {
        actions.repeatBackwardSearch(totalCount, operator, registerKey);
      }
      return remainder;
    }

      // Handle h/l/wW/eE motions (character left and right)
      // with no operator or with d/c/y operators
    matcher = getNormalMatcher("^(\"([0-9a-zA-Z\\-\"]))?(\\d*)([dcy]?)(\\d*)([hlwWeEbB])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString1 = matcher.group(3);
      String operator = matcher.group(4);
      String countString2 = matcher.group(5);
      String motion = matcher.group(6);
      String remainder = matcher.group(7);
      int totalCount = determineTotalCount(countString1, countString2);
      if (motion.equals("h")) {
        actions.normalModeBackwardChar(operator, totalCount, registerKey);
      } else if (motion.equals("l")) {
        actions.normalModeForwardChar(operator, totalCount, registerKey);
      } else if (motion.toLowerCase().equals("w") || motion.toLowerCase().equals("e")) {
        actions.normalModeForwardWord(operator, motion, totalCount, registerKey);
      } else if (motion.toLowerCase().equals("b")) {
        actions.normalModeBackwardWord(operator, motion, totalCount, registerKey);
      }
      return remainder;

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments

      // return sequence.replaceFirst(entireMatchString, "");
    }

    matcher = getNormalMatcher("^<TAB>(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.normalModeTab();
      return remainder;
    }

    matcher = getNormalMatcher("^<S-TAB>(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.normalModeShiftTab();
      return remainder;
    }

    if (sequence.matches(".*<ESC><ESC>")) {
      return "";
    }

    // If any of the above regexps "hits end" (the input reaches
    // its end before it could be determined if it would match
    // the regexp if additional characters were added to it), we
    // should return the sequence as is, for further user input
    // to be added
    Log.log("Normal matchers:");
    for (Matcher m : normalMatchers) {
      Log.log(m.toString());
      if (m.hitEnd()) {
        Log.log("Partial match found: " + m.toString());
        Log.log("normalMatchers length: " + normalMatchers.size());
        return sequence;
      }
    }

    // TODO: also need to account for ";" and "," (single char)
    return "";
  }

  private String evaluateVisualSequence() {
    Matcher matcher;
    /*
     * Handle to/till (f/t) and forward and backward search. These two regex
     * sections must come first, since the following sections assume that they have
     * already been processed.
     *
     * The order of subsequent regexes shouldn't matter
     **/
    // To or till
    // matcher = Pattern.compile("^(\\d*)([fFTt])(.)(.*)").matcher(sequence);
    matcher = getVisualMatcher("^(\\d*)([fFTt])(.)(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String motion = matcher.group(2);
      String key = matcher.group(3);
      String remainder = matcher.group(4);

      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      if (motion.equals("f") || motion.equals("t")) {
        actions.visualModeGoForwardToChar(count, motion, key);
      } else {
        actions.visualModeGoBackwardToChar(count, motion, key);
      }
      return remainder;
    }

    matcher = getVisualMatcher("^([?/])(.*)", sequence);
    if (matcher.find()) {
      String operator = matcher.group(1);
      String remainder = matcher.group(2);
      Mode.SEARCH.activate();
      actions.normalModeActivateSearch(operator, Mode.VISUAL);
      return remainder;
    }

    // u/U - upcase/downcase
    matcher = getVisualMatcher("^([uU])(.*)", sequence);
    if (matcher.find()) {
      String operator = matcher.group(1);
      String remainder = matcher.group(2);
      actions.visualModeSwitchCase(operator);
      return remainder;
    }

    // Switch selection end
    matcher = getVisualMatcher("^[oO](.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.visualModeSwitchSelectionEnd();
      return remainder;
    }


    // r - little "r" replace
    matcher = getVisualMatcher("^\\d*r(.)(.*)", sequence);
    if (matcher.find()) {
      // Numbers before r will be ignored
      String key = matcher.group(1);
      String remainder = matcher.group(2);
      actions.visualModeReplace(key);
      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      return remainder;
    }

    matcher = getVisualMatcher("^(\"([0-9a-zA-Z\\-\"]))?([DCY])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String remainder = matcher.group(4);
      actions.visualModeBigDCY(operator, registerKey);
      return remainder;
    }

    matcher = getVisualMatcher("^([$0])(.*)", sequence);
    if (matcher.find()) {
      String motion = matcher.group(1);
      String remainder = matcher.group(2);
      actions.visualModeGoToSegmentBoundary(motion);
      return remainder;
    }


    // This regex does not account for the fact that an escape
    // will not always take you to normal mode (e.g.
    // it can also escape from another operation, like in the case of
    // a<ESC> or i<ESC>
    matcher = getVisualMatcher("^<ESC>(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);

      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      return remainder;
    }

    // What other key combinations should make us escape back
    // to normal mode?
    // I can also combine these cases in a single if statement
    matcher = getVisualMatcher("^\\d*v(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      return remainder;
    }

    matcher = getVisualMatcher("^(\"([0-9a-zA-Z\\-\"]))?([dxcy])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String remainder = matcher.group(4);
      actions.visualModeOperate(operator, registerKey);
      actions.clearVisualMarks();
      return remainder;
    }

    matcher = getVisualMatcher("^(\\d*)(\"([0-9a-zA-Z\\-\"]))?(\\d*)[pP](.*)", sequence);
    if (matcher.find()) {
      String countString1 = matcher.group(1);
      String registerKey = matcher.group(3);
      String countString2 = matcher.group(4);
      String remainder = matcher.group(5);

      int totalCount = determineTotalCount(countString1, countString2);
      actions.visualModePut(registerKey, totalCount);
      return remainder;
    }

    matcher = getVisualMatcher("^(\\d*)([nN])(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String motion = matcher.group(2);
      String remainder = matcher.group(3);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      if (motion.equals("n")) {
        actions.repeatForwardSearch(count, "", "");
      } else {
        actions.repeatBackwardSearch(count, "", "");
      }
      return remainder;
    }

    // Handle h/l motions (character left and right)
    matcher = getVisualMatcher("^(\\d*)([hlwWeEbB])(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String motion = matcher.group(2);
      String remainder = matcher.group(3);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      if (motion.equals("h")) {
        actions.visualModeBackwardChar(count);
      } else if (motion.equals("l")) {
        actions.visualModeForwardChar(count);
      } else if (motion.toLowerCase().equals("w") || motion.toLowerCase().equals("e")) {
        actions.visualModeForwardWord(motion, count);
      } else if (motion.toLowerCase().equals("b")) {
        actions.visualModeBackwardWord(motion, count);
      }
      return remainder;
      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments
    }

    // Text object selection
    matcher = getVisualMatcher("^([ia])([wW\"\'\\[\\]\\{\\}<>\\(\\)])(.*)", sequence);
    if (matcher.find()) {
      String selector = matcher.group(1);
      String object = matcher.group(2);
      String remainder = matcher.group(3);
      actions.visualModeTextObjectSelection(selector, object);
      return remainder;
    }

    if (sequence.matches(".*<ESC><ESC>")) {
      return "";
    }

    // If any of the above regexps "hits end" (the input reaches
    // its end before it could be determined if it would match
    // the regexp if additional characters were added to it), we
    // should return the sequence as is, for further user input
    // to be added
    for (Matcher m : visualMatchers) {
      if (m.hitEnd()) {
        return sequence;
      }
    }

    // Sequence is invalid and evaluation will be aborted
    return "";
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

  int determineTotalCount(String countString1, String countString2) {
    int totalCount = 1;
    int count1 = (countString1.equals("") || countString1 == null || countString1.equals("1")) ? 0 : Integer.parseInt(countString1, 10);
    int count2 = (countString2.equals("") || countString2 == null || countString2.equals("1")) ? 0 : Integer.parseInt(countString2, 10);

    if (count1 + count2 == 0) {
      totalCount = 1;
    } else {
      totalCount = count1 + count2;
    }
    return totalCount;
  }
}
