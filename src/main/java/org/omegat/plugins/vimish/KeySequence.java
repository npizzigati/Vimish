package org.omegat.plugins.vimish;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

class KeySequence {
  private static KeySequence instance = null;
  private String sequence = "";

  private KeySequence() {}

  static KeySequence getKeySequence() {
    if (instance == null) {
      instance = new KeySequence();
    } 
    return instance;
  }

  void apply(String keyString) {
    sequence += keyString;
    if (Mode.NORMAL.isActive()) {
      evaluateNormalSequence();
    } else if (Mode.INSERT.isActive()) {
      evaluateInsertSequence();
    } else if (Mode.VISUAL.isActive()) {
      evaluateVisualSequence();
    }
  }

  void evaluateVisualSequence(String sequence) {
    // Handle case where a number is entered before the v
    // (to switch from normal into visual mode),
    // entering into visual mode with a count
    Matcher match = Pattern.compile("(\\d+)v").matcher(sequence);
    match.find();
    String countString = match.group(1);
    int count = Integer.parseInt(countString, 10);

    Actions.visualForwardChar(count - 1);
    resetSequence();
  }

  void evaluateVisualSequence() {
    // This regex does not account for the fact that an escape
    // will not always take you to normal mode (e.g.
    // it can also escape from another operation, like in the vase of
    // aESC or iESC
    if (sequence.matches(".*ESC")) {
      Mode.NORMAL.activate();
      resetSequence();
    } else if (sequence.matches("\\d+v")) {
      // What other key combinations should make us escape back
      // to normal mode?
      // I can also combine these cases in a single if statement
      Mode.NORMAL.activate();
      resetSequence();
    } else if (sequence.equals("d") || sequence.equals("x")) {
      Actions.visualDelete();
      Mode.NORMAL.activate();
      resetSequence();
    } else if (sequence.equals("c")) {
      Actions.visualDelete();
      Mode.INSERT.activate();
      resetSequence();
    } else if (sequence.equals("y")) {
      Actions.visualYank();
      Mode.NORMAL.activate();
      resetSequence();
    } else if (sequence.matches("^\\d*[hl]$")) {
      // Handle h/l motions (character left and right)
      Matcher match = Pattern.compile("^(\\d*)([hl])$").matcher(sequence);
      match.find();
      String countString = match.group(1);
      String motion = match.group(2);

      int count = (countString.equals("")) ? 1 : Integer.parseInt(countString, 10);

      if (motion.equals("h")) {
        Actions.visualBackwardChar(count);
      } else if (motion.equals("l")) {
        Actions.visualForwardChar(count);
      }

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments
      resetSequence();
    }
  }

  void evaluateInsertSequence() {
    if (sequence.equals("ESC")) {
      Mode.NORMAL.activate();
      resetSequence();
    }
  }

  void evaluateNormalSequence() {
    if (sequence.matches("[^fFTt?/]*i")) {
      Mode.INSERT.activate();
      resetSequence();
    } else if (sequence.matches("[^fFTt?/]*u")) {
      Actions.undo();
      resetSequence();
    } else if (sequence.matches("^\\d+v")) {
      // If a number is entered followed by a "v", change to visual mode
      // without resetting sequence, so as to evaluate that
      // sequence (preceding number is a count)
      Mode.VISUAL.activate();
      evaluateVisualSequence(sequence);
    } else if (sequence.matches("^[^fFTt?/]*v")) {
      // If just a "v" is entered with no preceding number,
      // change to visual mode without resetting sequence, so as
      // to mark character that caret is on (count is implicitly 1)
      Mode.VISUAL.activate();
      evaluateVisualSequence("1v");
    } else if (sequence.matches("^[^fFTt?/]*p")) {
      Actions.normalPut("after");
      resetSequence();
    } else if (sequence.matches("^[^fFTt?/]*P")) {
      Actions.normalPut("before");
      resetSequence();
    } else if (sequence.matches("^\\d*[dcy]?\\d*[hl]$")) {
      // Handle h/l motions (character left and right)
      // with no operator or with d/c/y operators
      Matcher match = Pattern.compile("^(\\d*)([dcy]?)(\\d*)([hl])$").matcher(sequence);
      match.find();
      String countString1 = match.group(1);
      String operator = match.group(2);
      String countString2 = match.group(3);
      String motion = match.group(4);

      int totalCount = determineTotalCount(countString1, countString2);

      if (motion.equals("h")) {
        Actions.normalBackwardChar(operator, totalCount);
      } else if (motion.equals("l")) {
        Actions.normalForwardChar(operator, totalCount);
      }

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments
      resetSequence();
    } else if (sequence.matches("^\\d*[ftFT].$")) {
      // Handle "to" (F/f) and "till" (T/t)
      // Matcher matches = Pattern.compile("^(\\d*)[ftFT].$").matcher(sequence);
      int length = sequence.length();

      int number = 1;
      if (length > 2) number = Integer.parseInt(sequence.substring(0, length - 2), 10); 

      String letter = String.valueOf(sequence.charAt(length - 2));
      String searchChar = String.valueOf(sequence.charAt(length - 1));

      resetSequence();
    } else if (sequence.matches("^b$")) {
      // Execute backward char
      resetSequence();
    } else if (sequence.matches("^w$")) {
      // Execute forward word
      resetSequence();
    } else if (sequence.matches("^\\d+w$")) {
      int number = Integer.parseInt(sequence.substring(0, sequence.length() - 1), 10);
      resetSequence();
      // Execute forward word with number as argument
      JOptionPane.showMessageDialog(null, number);
      resetSequence();
    } else if (sequence.length() > 15) {
      resetSequence();
    }
    // } else if (sequence.matches("^.+[^hl0$^]$")) {
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
    instance = null;
  }
}
