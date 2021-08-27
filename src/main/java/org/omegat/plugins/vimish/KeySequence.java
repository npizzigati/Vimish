package org.omegat.plugins.vimish;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.omegat.util.Log;

class KeySequence {
  private String sequence = "";
  private Actions actions;
  private int actionsCount = 0;
  private Configuration configuration;

  KeySequence(Actions actions) {
    this.actions = actions;
    configuration = Configuration.getConfiguration();
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
    apply("");
    // TODO: Extract the evaluation part of this
    // method (and the recursive calls) to an
    // "evaluate" method
  }

  // private void evaluateVisualSequence(String sequence) {
  //   // Handle case where a number is entered before the v
  //   // (to switch from normal into visual mode),
  //   // entering into visual mode with a count
  //   Log.log("EVALUATING VISUAL SEQUENCE");
  //   Matcher match = Pattern.compile("^(\\d+)v(.*)").matcher(sequence);
  //   match.find();
  //   String countString = match.group(1);
  //   String remainder = match.group(2);
  //   int count = Integer.parseInt(countString, 10);

  //   actions.visualModeForwardChar(count - 1);
  //   sequence = remainder;
  // }

  private void evaluateVisualSequence() {
    // This regex does not account for the fact that an escape
    // will not always take you to normal mode (e.g.
    // it can also escape from another operation, like in the vase of
    // a<ESC> or i<ESC>
    if (sequence.matches("^<ESC>.*")) {
      Matcher match = Pattern.compile("^<ESC>(.*)")
                             .matcher(sequence);
      match.find();
      String remainder = match.group(1);

      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      sequence = remainder;
    } else if (sequence.matches("^\\d*v.*")) {
      // What other key combinations should make us escape back
      // to normal mode?
      // I can also combine these cases in a single if statement
      Matcher match = Pattern.compile("^\\d*v(.*)")
                             .matcher(sequence);
      match.find();
      String remainder = match.group(1);

      Mode.NORMAL.activate();
      actions.clearVisualMarks();
      sequence = remainder;
    } else if (sequence.matches("^[dx].*")) {
      Matcher match = Pattern.compile("^[dx](.*)")
                             .matcher(sequence);
      match.find();
      String remainder = match.group(1);

      actions.visualModeDelete();
      actions.clearVisualMarks();
      Mode.NORMAL.activate();
      sequence = remainder;
    } else if (sequence.matches("^c.*")) {
      Matcher match = Pattern.compile("^c(.*)")
                             .matcher(sequence);
      match.find();
      String remainder = match.group(1);

      actions.visualModeDelete();
      actions.clearVisualMarks();
      Mode.INSERT.activate();
      sequence = remainder;
    } else if (sequence.matches("^y.*")) {
      Matcher match = Pattern.compile("^y(.*)")
                             .matcher(sequence);
      match.find();
      String remainder = match.group(1);

      actions.visualModeYank();
      actions.clearVisualMarks();
      Mode.NORMAL.activate();
      sequence = remainder;
    } else if (sequence.matches("^\\d*[hl].*")) {
      // Handle h/l motions (character left and right)
      Matcher match = Pattern.compile("^(\\d*)([hl])(.*)").matcher(sequence);
      match.find();
      String countString = match.group(1);
      String motion = match.group(2);
      String remainder = match.group(3);

      int count = (countString.equals("")) ? 1 : Integer.parseInt(countString, 10);

      if (motion.equals("h")) {
        actions.visualModeBackwardChar(count);
      } else if (motion.equals("l")) {
        actions.visualModeForwardChar(count);
      }
      sequence = remainder;

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments
    } else if (sequence.matches(".*<ESC><ESC>")) {
      sequence = "";
    }
  }

  private void evaluateInsertSequence() {
    if (sequence.matches("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<ENTER>).*")) {
      Matcher match = Pattern.compile("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<ENTER>)(.*)")
                             .matcher(sequence);
      match.find();
      String key = match.group(1);
      String remainder = match.group(2);
      switch (key) {
        case "<ESC>":
          Log.log("Escape evaluated");
          Mode.NORMAL.activate();
          if (configuration.getConfigMoveCursorBack()) {
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
        case "<ENTER>":
          actions.insertModeEnter();
          Log.log("Enter evaluated");
          break;
        case "<DEL>":
          actions.insertModeDelete();
          Log.log("Delete evaluated");
          break;
      }
      sequence = remainder;
    } else {
      String character = sequence.substring(0, 1);
      String remainder = sequence.substring(1);
      actions.insertModeInsertText(character);
      sequence = remainder;
    }
  }

  // private String removeEvaluatedPart(String sequence, String entireMatchString) {
  //   return sequence.replaceFirst(entireMatchString, "");
  // }

  private void evaluateNormalSequence() {
    /*
     * Handle to/till (f/t) and forward and backword search.
     * These two regex sections must come first, since the following
     * sections assume that they have already been processed.
     *
     * The order of subsequent regexes shouldn't matter
     **/
    // To or till or search
    if (sequence.matches("^[fFTt]..*")) {
      // Need to fill this in
      Matcher match = Pattern.compile("(^[fFTt])(.)(.*)")
                             .matcher(sequence);
      match.find();
      // String key = match.group(2);
      // String remainder = match.group(3);

      // Fill in action

      // sequence = remainder;
    }

    else if (sequence.matches("^[?/].+<ENTER>.*")) {
      // Need to fill this in
      Matcher match = Pattern.compile("^([?/])(.+)<ENTER>(.*)")
                             .matcher(sequence);
      match.find();

      // // Fill in action

      // sequence = remainder;
    }

    else if (sequence.matches("^i.*")) {
      Matcher match = Pattern.compile("^i(.*)")
                             .matcher(sequence);
      match.find();
      String remainder = match.group(1);

      Mode.INSERT.activate();
      sequence = remainder;
    }

    else if (sequence.matches("^a.*")) {
      Matcher match = Pattern.compile("^a(.*)")
                             .matcher(sequence);
      match.find();
      String remainder = match.group(1);

      actions.normalModeForwardChar("", 1);
      Mode.INSERT.activate();
      sequence = remainder;
    }

    else if (sequence.matches("^u.*")) {
      Matcher match = Pattern.compile("^u(.*)")
                             .matcher(sequence);
      match.find();
      String remainder = match.group(1);

      actions.undo();
      sequence = remainder;
    }

    else if (sequence.matches("^\\d*v.*")) {
      Matcher match = Pattern.compile("^(\\d*)v(.*)")
                             .matcher(sequence);
      match.find();
      String entireMatch = match.group(0);
      Log.log("entireMatch: " + entireMatch);
      String countString = match.group(1);
      Log.log("countString: " + countString);
      int count = (countString.equals("")) ? 0 : Integer.parseInt(countString, 10);
      String remainder = match.group(2);

      Mode.VISUAL.activate();
      if (count == 0) {
        actions.visualModeForwardChar(1);
        actions.visualModeBackwardChar(1);
      } else {
        actions.visualModeForwardChar(count - 1);
      }
      sequence = remainder;
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
      Matcher match = Pattern.compile("^\"([0-9a-zA-Z-])([pP])(.*)")
                             .matcher(sequence);
      Log.log("Named register put");
      match.find();
      String registerKey = match.group(1);
      String putLetter = match.group(2);
      String remainder = match.group(3);
      String position = (putLetter.equals("p")) ? "after" : "before";

      actions.normalModePutSpecificRegister(registerKey, position);
      sequence = remainder;
    } else if (sequence.matches("^([pP]|\"\"[pP]).*")) {

      Matcher match = Pattern.compile("^(([pP])|\"\"([pP]))(.*)")
                             .matcher(sequence);
      match.find();
      String putLetter = (match.group(2).equals("")) ? match.group(3) : match.group(2);
      String remainder = match.group(4);
      String position = (putLetter.equals("p")) ? "after" : "before";

      actions.normalModePutUnnamedRegister(position);
      sequence = remainder;
    }

    else if (sequence.matches("^\\d*[dcy]?\\d*[hl].*")) {

      // Handle h/l motions (character left and right)
      // with no operator or with d/c/y operators
      Matcher match = Pattern.compile("^(\\d*)([dcy]?)(\\d*)([hl])(.*)")
                             .matcher(sequence);
      match.find();
      String entireMatch = match.group(0);
      Log.log("sequence: " + sequence);
      Log.log("entireMatch: " + entireMatch);
      String countString1 = match.group(1);
      String operator = match.group(2);
      String countString2 = match.group(3);
      String motion = match.group(4);
      String remainder = match.group(5);

      int totalCount = determineTotalCount(countString1, countString2);

      if (motion.equals("h")) {
        actions.normalModeBackwardChar(operator, totalCount);
      } else if (motion.equals("l")) {
        actions.normalModeForwardChar(operator, totalCount);
      }
      sequence = remainder;

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments

      // sequence = sequence.replaceFirst(entireMatchString, "");
    } else if (sequence.matches("^<TAB>.*")) {
      Matcher match = Pattern.compile("^<TAB>(.*)")
                             .matcher(sequence);
      match.find();
      String remainder = match.group(1);
      sequence = remainder;
      // TODO: Need to implement shift-tab to go to previous tab
      actions.normalModeTab();
      // TODO: Need to fix up the cases below
    } else if (sequence.matches(".*<ESC><ESC>")) {
      sequence = "";
    }
    // TODO: also need to account for ";" and "," (single char)
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
