package org.omegat.plugins.vimish;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

class KeySequence {
  private static KeySequence instance = null;
  private String sequence = "";

  private KeySequence() {
  }

  static KeySequence getKeySequence() {
    if (instance == null) {
      instance = new KeySequence();
    } 
    return instance;
  }

  void applyKey(String keyString) {
    sequence += keyString;

    if (sequence.matches("^\\d*[dcy]?\\d*[hl]$")) {

      // Handle h/l motions (character left and right)
      // with no operator or with d/c/y operators
      int length = sequence.length();
      
      Matcher match = Pattern.compile("^(\\d*)([dcy]?)(\\d*)([hl])$").matcher(sequence);
      match.find();
      String countString1 = match.group(1);
      String operator = match.group(2);
      String countString2 = match.group(3);
      String motion = match.group(4);

      int totalCount = determineTotalCount(countString1, countString2);

      if (motion.equals("h")) {
        Actions.backwordChar(operator, totalCount);
      } else if (motion.equals("l")) {
        Actions.forwardChar(operator, totalCount);
      }

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments
      resetSequence();
    }
    // TODO: Need to handle u and c-r (undo and redo)
    if (sequence.matches("^\\d*[ftFT].$")) {
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
