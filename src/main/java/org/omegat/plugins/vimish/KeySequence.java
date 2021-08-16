package org.omegat.plugins.vimish;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.omegat.util.Log;

class KeySequence {
  private String sequence = "";
  private Actions actions;
  private int actionsCount = 0;
  
  KeySequence(Actions actions) {
    this.actions = actions;
  }

  void apply(String keyString) {
    sequence += keyString;
    actionsCount += 1;

    // Count number of actions applied to prevent runaway
    if (sequence.equals("") || actionsCount > 10) {
      actionsCount = 0;
      return;
    }

    if (Mode.NORMAL.isActive()) {
      evaluateNormalSequence();
    } else if (Mode.INSERT.isActive()) {
      evaluateInsertSequence();
    } else if (Mode.VISUAL.isActive()) {
      evaluateVisualSequence();
    }

    // Recursively call apply on rest of sequence
    // with no additional keyString
    keyString = "";
    apply(keyString);
    // TODO: Extract the evaluation part of this
    // method (and the recursive calls) to an
    // "evaluate" method
  }

  private void evaluateVisualSequence(String sequence) {
    // Handle case where a number is entered before the v
    // (to switch from normal into visual mode),
    // entering into visual mode with a count
    Log.log("EVALUATING VISUAL SEQUENCE");
    Matcher match = Pattern.compile("^(\\d+)v").matcher(sequence);
    match.find();
    String countString = match.group(1);
    int count = Integer.parseInt(countString, 10);

    actions.visualModeForwardChar(count - 1);
    resetSequence();
  }

  private void evaluateVisualSequence() {
    // This regex does not account for the fact that an escape
    // will not always take you to normal mode (e.g.
    // it can also escape from another operation, like in the vase of
    // a<ESC> or i<ESC>
    if (sequence.matches("^<ESC>")) {
      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      resetSequence();
    } else if (sequence.matches("^\\d+v")) {
      // What other key combinations should make us escape back
      // to normal mode?
      // I can also combine these cases in a single if statement
      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      resetSequence();
    } else if (sequence.matches("^[dx]")) {
      actions.visualModeDelete();
      actions.clearVisualMarks();
      Mode.NORMAL.activate();
      resetSequence();
    } else if (sequence.matches("^c")) {
      actions.visualModeDelete();
      actions.clearVisualMarks();
      Mode.INSERT.activate();
      resetSequence();
    } else if (sequence.matches("^y")) {
      actions.visualModeYank();
      actions.clearVisualMarks();
      Mode.NORMAL.activate();
      resetSequence();
    } else if (sequence.matches("^\\d*[hl]")) {
      // Handle h/l motions (character left and right)
      Matcher match = Pattern.compile("^(\\d*)([hl])").matcher(sequence);
      match.find();
      String countString = match.group(1);
      String motion = match.group(2);

      int count = (countString.equals("")) ? 1 : Integer.parseInt(countString, 10);

      if (motion.equals("h")) {
        actions.visualModeBackwardChar(count);
      } else if (motion.equals("l")) {
        actions.visualModeForwardChar(count);
      }

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments
      resetSequence();
    }
  }

  private void evaluateInsertSequence() {
    if (sequence.matches("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<ENTER>).*")) {
      Matcher match = Pattern.compile("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<ENTER>)")
                             .matcher(sequence);
      match.find();
      String entireMatchString = match.group(0);
      switch (entireMatchString) {
        case "<ESC>":
          Log.log("Escape evaluated");
          Mode.NORMAL.activate();
          break;
        case "<BACKSPACE>":
          actions.insertModeBackspace();
          Log.log("Backspace evaluated");
          break;
        case "<TAB>":
          actions.insertModeTab();
          Log.log("Tab evaluated");
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
      sequence = removeEvaluatedPart(sequence, entireMatchString);
    } else {
      actions.insertModeInsertText(sequence);
      sequence = "";
    }
  }

  private String removeEvaluatedPart(String sequence, String entireMatchString) {
    return sequence.replaceFirst(entireMatchString, "");
  }

  private void evaluateNormalSequence() {
    /* 
     * Handle to/till (f/t) and forward and backword search.
     * This regex section must come first, since the following
     * sections assume that is has already been processed.
     *
     * The order of subsequent regexes shouldn't matter    
     **/
    // To or till or search
    if (sequence.matches("^([fFTt].|[?/]..*)")) {
      // Need to fill this in
      resetSequence();
    }

    else if (sequence.matches("^.*i")) {
      Mode.INSERT.activate();
      resetSequence();
    }

    else if (sequence.matches("^.*u")) {
      actions.undo();
      resetSequence();
    }

    else if (sequence.matches("^\\d+v")) {
      // If a number is entered followed by a "v", change to visual mode
      // without resetting sequence, so as to evaluate that
      // sequence (preceding number is a count)
      Mode.VISUAL.activate();
      evaluateVisualSequence(sequence);
    } else if (sequence.matches("^.*v")) {
      // If just a "v" is entered with no preceding number,
      // change to visual mode without resetting sequence, so as
      // to mark character that caret is on (count is implicitly 1)
      Mode.VISUAL.activate();
      evaluateVisualSequence("1v");
    }

    /*
     * Put
     **/
    else if (sequence.matches("^.*\"[0-9a-zA-Z-]p")) {
      Matcher match = Pattern.compile("^.*\"([0-9a-zA-Z-])p")
                             .matcher(sequence);
      match.find();
      String registerKey = match.group(1);
      String position = "after";
      actions.normalModePutSpecificRegister(registerKey, position);
      resetSequence();
    } else if (sequence.matches("^(.*p|.*\"\"p)")) {
      actions.normalModePutUnnamedRegister("after");
      resetSequence();
    } else if (sequence.matches("^(.*P|.*\"\"P)")) {
      actions.normalModePutUnnamedRegister("before");
      resetSequence();
    }

    else if (sequence.matches("^\\d*[dcy]?\\d*[hl].*")) {

      // Handle h/l motions (character left and right)
      // with no operator or with d/c/y operators
      Matcher match = Pattern.compile("^(\\d*)([dcy]?)(\\d*)([hl])")
                             .matcher(sequence);
      match.find();
      String entireMatchString = match.group(0);
      String countString1 = match.group(1);
      String operator = match.group(2);
      String countString2 = match.group(3);
      String motion = match.group(4);

      int totalCount = determineTotalCount(countString1, countString2);

      if (motion.equals("h")) {
        actions.normalBackwardChar(operator, totalCount);
      } else if (motion.equals("l")) {
        actions.normalModeForwardChar(operator, totalCount);
      }

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments

      // Remove match from beginning of sequence and evaluate
      // rest of sequence
      Log.log("entireMatchString: '" + entireMatchString + "'");
      sequence = sequence.replaceFirst(entireMatchString, "");
    } else if (sequence.matches("^<TAB>.*")) {
      Matcher match = Pattern.compile("^(<TAB>).*")
                             .matcher(sequence);
      match.find();
      String tabMatchString = match.group(1);
      sequence = removeEvaluatedPart(sequence, tabMatchString);
      actions.normalModeTab();
      // TODO: Need to fix up the cases below
    } else if (sequence.matches("^b")) {
      // Execute backward char
      resetSequence();
    } else if (sequence.matches("^w")) {
      // Execute forward word
      resetSequence();
    } else if (sequence.matches("^\\d+w")) {
      int number = Integer.parseInt(sequence.substring(0, sequence.length() - 1), 10);
      resetSequence();
      // Execute forward word with number as argument
      resetSequence();
    } else if (sequence.length() > 15) {
      resetSequence();
    }
    // } else if (sequence.matches("^.+[^hl0$^]")) {
    //   // reset sequence if it ends with a non-motion character
    //   // that is not preceded by "f" or "t" ("f" and "t" checked
    //   // for in earlier case)
    //   // TODO: also need to account for ";" and "," (single char)
    // }
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

  void resetSequence() {
    sequence = "";
  }
}
