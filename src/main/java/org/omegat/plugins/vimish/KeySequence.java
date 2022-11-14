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

  private class LastChange {
    String baseSequence;
    String registerKey;
    Integer count;

    LastChange(String baseSequence, String registerKey, Integer count) {
      this.baseSequence = baseSequence;
      this.count = count;
    }

    void append(String key) {
      baseSequence += key;
    }

    /**
     * Using the arrow keys in insert or replace mode will reset
     * the base sequence to "i"
     */
    void arrowKeyReset() {
      baseSequence = "i";
    }

    void deleteLastKey() {
      if (!Util.isEmpty(baseSequence)) {
        baseSequence = baseSequence.substring(0, baseSequence.length() - 1);
      }
    }
  }

  private LastChange lastChange;
  private LastChange pendingLastChange;

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
    // To or till find
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?(\\d*)([dcy]?)(\\d*)([fFTt])(.)(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString1 = matcher.group(3);
      String operator = matcher.group(4);
      String countString2 = matcher.group(5);
      String motion = matcher.group(6);
      String character = matcher.group(7);
      String remainder = matcher.group(8);
      int count = determineTotalCount(countString1, countString2);
      if (motion.equals("f") || motion.equals("t")) {
        actions.normalModeGoToChar(count, operator, motion, character, registerKey, RepeatType.NONE);
      } else {
        actions.normalModeGoToChar(count, operator, motion, character, registerKey, RepeatType.NONE);
      }
      if (operator.equals("d") || operator.equals("c")) {
        String lastChangeSequence = operator + motion + character;
        lastChange = new LastChange(lastChangeSequence, registerKey, count);
      }
      return remainder;
    }

    // Search
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?(\\d*)([dcy]?)(\\d*)([?/])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString1 = matcher.group(3);
      String operator = matcher.group(4);
      String countString2 = matcher.group(5);
      String searchOperator = matcher.group(6);
      String remainder = matcher.group(7);
      int count = determineTotalCount(countString1, countString2);
      Mode.SEARCH.activate();
      actions.activateSearch(count, operator, searchOperator, registerKey, Mode.NORMAL);
      if (operator.equals("d") || operator.equals("c")) {
        String lastChangeSequence = operator + searchOperator;
        pendingLastChange = new LastChange(lastChangeSequence, registerKey, count);
      }
      return remainder;
    }

    // r - little "r" replace
    matcher = getNormalMatcher("^(\\d*)r(.)(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String character = matcher.group(2);
      String remainder = matcher.group(3);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeReplace(character, count);
      String lastChangeSequence = "r" + character;
      lastChange = new LastChange(lastChangeSequence, null, count);
      return remainder;
    }

    // R - big "R" replace
    matcher = getNormalMatcher("^R(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      Mode.REPLACE.activate();
      String lastChangeSequence = "R";
      lastChange = new LastChange(lastChangeSequence, null, null);
      return remainder;
    }

    // Text object selection (e.g. "diw" -> delete in word;
    // "di)" -> delete in parentheses)
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?([dcy])([ia])([wW\"\'\\[\\]\\{\\}<>\\(\\)])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String selector = matcher.group(4);
      String object = matcher.group(5);
      String remainder = matcher.group(6);
      actions.normalModeTextObjectSelection(operator, selector, object, registerKey);
      if (operator.equals("d") || operator.equals("c")) {
        String lastChangeSequence = operator + selector + object;
        lastChange = new LastChange(lastChangeSequence, registerKey, null);
      }
      return remainder;
    }

    // dd/cc/yy full-line operation
    // Ignore count since there are no line-wise actions in Vimish
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?\\d*(dd|cc|yy)(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String remainder = matcher.group(4);
      actions.normalModeFullLineOperation(operator, registerKey);
      if (operator.equals("dd") || operator.equals("cc")) {
        String lastChangeSequence = operator;
        lastChange = new LastChange(lastChangeSequence, registerKey, null);
      }
      return remainder;
    }

    // Big D/C rest-of-line operations, and big Y full-line operation
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?([DCY])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String remainder = matcher.group(4);
      actions.normalModeBigDCY(operator, registerKey);
      if (operator.equals("D") || operator.equals("C")) {
        String lastChangeSequence = operator;
        lastChange = new LastChange(lastChangeSequence, registerKey, null);
      }
      return remainder;
    }

    matcher = getNormalMatcher("^([$0])(.*)", sequence);
    if (matcher.find()) {
      String motion = matcher.group(1);
      String remainder = matcher.group(2);
      actions.normalModeGoToSegmentBoundary(motion);
      return remainder;
    }

    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?([dcy])([$0])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String motion = matcher.group(4);
      String remainder = matcher.group(5);
      actions.normalModeOperateToSegmentBoundary(operator, motion, registerKey);
      if (operator.equals("d") || operator.equals("c")) {
        String lastChangeSequence = operator + motion;
        lastChange = new LastChange(lastChangeSequence, registerKey, null);
      }
      return remainder;
    }

    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?(\\d*)x(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString = matcher.group(3);
      String remainder = matcher.group(4);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeForwardChar("d", count, registerKey);
      String lastChangeSequence = "x";
      lastChange = new LastChange(lastChangeSequence, registerKey, count);
      return remainder;
    }

    // s
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?(\\d*)s(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString = matcher.group(3);
      String remainder = matcher.group(4);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeForwardChar("c", count, registerKey);
      String lastChangeSequence = "s";
      lastChange = new LastChange(lastChangeSequence, registerKey, count);
      return remainder;
    }

    // S
    // Ignore count since there are no line-wise actions in Vimish
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?\\d*S(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String remainder = matcher.group(3);
      actions.normalModeFullLineOperation("cc", registerKey);
      String lastChangeSequence = "S";
      lastChange = new LastChange(lastChangeSequence, registerKey, null);
      return remainder;
    }

    matcher = getNormalMatcher("^\\d*i(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      Mode.INSERT.activate();
      String lastChangeSequence = "i";
      lastChange = new LastChange(lastChangeSequence, null, null);
      return remainder;
    }

    // Append
    matcher = getNormalMatcher("^\\d*a(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.normalModeAppendAfterCursor();
      String lastChangeSequence = "a";
      lastChange = new LastChange(lastChangeSequence, null, null);
      return remainder;
    }

    // Append at end
    matcher = getNormalMatcher("^\\d*A(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.normalModeAppendAtEnd();
      String lastChangeSequence = "A";
      lastChange = new LastChange(lastChangeSequence, null, null);
      return remainder;
    }

    matcher = getNormalMatcher("^(\\d*)~(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeToggleCase(count);
      String lastChangeSequence = "~";
      lastChange = new LastChange(lastChangeSequence, null, count);
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
      String putOperator = matcher.group(5);
      String remainder = matcher.group(6);

      int totalCount = determineTotalCount(countString1, countString2);
      actions.normalModePut(registerKey, putOperator, totalCount);
      String lastChangeSequence = putOperator;
      lastChange = new LastChange(lastChangeSequence, registerKey, totalCount);
      return remainder;
    }

    // Repeat search
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?(\\d*)([dcy]?)(\\d*)([nN])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString1 = matcher.group(3);
      String operator = matcher.group(4);
      String countString2 = matcher.group(5);
      String repeatMotion = matcher.group(6);
      String remainder = matcher.group(7);
      int totalCount = determineTotalCount(countString1, countString2);
      actions.repeatSearch(totalCount, repeatMotion, operator, registerKey);
      if (operator.equals("d") || operator.equals("c")) {
        String lastChangeSequence = operator + repeatMotion;
        lastChange = new LastChange(lastChangeSequence, registerKey, totalCount);
      }
      return remainder;
    }

    // Repeat find
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?(\\d*)([dcy]?)(\\d*)([;,])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String countString1 = matcher.group(3);
      String operator = matcher.group(4);
      String countString2 = matcher.group(5);
      String repeatMotion = matcher.group(6);
      String remainder = matcher.group(7);
      int totalCount = determineTotalCount(countString1, countString2);
      actions.repeatFind(totalCount, repeatMotion, operator, registerKey);
      if (operator.equals("d") || operator.equals("c")) {
        String lastChangeSequence = operator + repeatMotion;
        lastChange = new LastChange(lastChangeSequence, registerKey, totalCount);
      }
      return remainder;
    }

      // Handle h/l/wW/eE motions (character left and right)
      // with no operator or with d/c/y operators
    matcher = getNormalMatcher("^(\"([\\w\\-\"]))?(\\d*)([dcy]?)(\\d*)([hlwWeEbB])(.*)", sequence);
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
      if (operator.equals("d") || operator.equals("c")) {
        String lastChangeSequence = operator + motion;
        lastChange = new LastChange(lastChangeSequence, registerKey, totalCount);
      }
      return remainder;

      // We are currently ignoring motion keys j/k and uppercase
      // H/L/J/K, since these are not particularly useful for
      // translation segments (which contain no newlines)
      // NOTE: H/M/L (high/middle/low) may be useful for long segments

      // return sequence.replaceFirst(entireMatchString, "");
    }

    // Backspace
    matcher = getNormalMatcher("^(\\d*)<BACKSPACE>(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeBackwardChar(count);
      return remainder;
    }

    // Left
    matcher = getNormalMatcher("^(\\d*)<LEFT>(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeBackwardChar(count);
      return remainder;
    }

    // Right
    matcher = getNormalMatcher("^(\\d*)<RIGHT>(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeForwardChar(count);
      return remainder;
    }

    // Space
    matcher = getNormalMatcher("^(\\d*) (.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.normalModeForwardChar(count);
      return remainder;
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

    // Repeat last change
    matcher = getNormalMatcher("^(\\d*)\\.(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      if (lastChange == null) {
        return remainder;
      }
      String newCountString = countString;
      if (Util.isEmpty(newCountString)) {
        if (lastChange.count == null) {
          newCountString = "";
        } else {
          newCountString = Integer.toString(lastChange.count);
        }
      }
      String registerKey = lastChange.registerKey;
      if (Util.isEmpty(registerKey)) {
        registerKey = "";
      }
      // Add escape after base sequence to be sure we are back in normal mode when done
      return registerKey + newCountString + lastChange.baseSequence + "<ESC>" + remainder;
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
      actions.visualModeGoToChar(count, motion, key, RepeatType.NONE);
      return remainder;
    }

    // Search
    matcher = getVisualMatcher("^(\\d*)([?/])(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String searchOperator = matcher.group(2);
      String remainder = matcher.group(3);
      Mode.SEARCH.activate();
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.activateSearch(count, "", searchOperator, "", Mode.VISUAL);
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

    // Toggle case
    matcher = getVisualMatcher("^\\d*~(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.visualModeSwitchCase("");
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

    matcher = getVisualMatcher("^(\"([\\w\\-\"]))?([DCSY])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String remainder = matcher.group(4);
      actions.visualModeBigDCSY(operator, registerKey);
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

    matcher = getVisualMatcher("^(\"([\\w\\-\"]))?([dxcsy])(.*)", sequence);
    if (matcher.find()) {
      String registerKey = matcher.group(2);
      String operator = matcher.group(3);
      String remainder = matcher.group(4);
      actions.visualModeOperate(operator, registerKey);
      actions.clearVisualMarks();
      return remainder;
    }

    // Append
    matcher = getVisualMatcher("^\\d*A(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.visualModeAppendAfterCursor();
      // Visual mode "A" behaves like normal mode "a" (insert
      // mode started at caret position + 1)
      String lastChangeSequence = "a";
      lastChange = new LastChange(lastChangeSequence, null, null);
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

    // Repeat search
    matcher = getVisualMatcher("^(\\d*)([nN])(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String motion = matcher.group(2);
      String remainder = matcher.group(3);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.repeatSearch(count, motion, "", "");
      return remainder;
    }

    // Repeat find
    matcher = getVisualMatcher("^(\\d*)([;,])(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String repeatMotion = matcher.group(2);
      String remainder = matcher.group(3);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.repeatFind(count, repeatMotion, "", "");
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

    // Backspace
    matcher = getVisualMatcher("^(\\d*)<BACKSPACE>(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.visualModeBackwardChar(count);
      return remainder;
    }

    // Left
    matcher = getVisualMatcher("^(\\d*)<LEFT>(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.visualModeBackwardChar(count);
      return remainder;
    }

    // Right
    matcher = getVisualMatcher("^(\\d*)<RIGHT>(.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.visualModeForwardChar(count);
      return remainder;
    }

    // Space
    matcher = getVisualMatcher("^(\\d*) (.*)", sequence);
    if (matcher.find()) {
      String countString = matcher.group(1);
      String remainder = matcher.group(2);
      int count = (countString.equals("") || countString == null) ? 1 : Integer.parseInt(countString, 10);
      actions.visualModeForwardChar(count);
      return remainder;
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

    matcher = getVisualMatcher("^<TAB>(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.visualModeTab();
      return remainder;
    }

    matcher = getVisualMatcher("^<S-TAB>(.*)", sequence);
    if (matcher.find()) {
      String remainder = matcher.group(1);
      actions.visualModeShiftTab();
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
    if (sequence.matches("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>|<LEFT>|<RIGHT>).*")) {
      Matcher matcher = Pattern.compile("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>|<LEFT>|<RIGHT>)(.*)").matcher(sequence);
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
          if (lastChange != null) {
            lastChange.append(key);
          }
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
        case "<LEFT>":
          actions.insertModeBackwardChar(1);
          if (lastChange != null) {
            lastChange.arrowKeyReset();
          }
          break;
        case "<RIGHT>":
          actions.insertModeForwardChar(1);
          if (lastChange != null) {
            lastChange.arrowKeyReset();
          }
          break;
        case "<DEL>":
          actions.replaceModeDelete();
          Log.log("Delete evaluated");
          if (lastChange != null) {
            lastChange.append(key);
          }
          break;
      }
      newSequence = remainder;
    } else {
      String character = sequence.substring(0, 1);
      String remainder = sequence.substring(1);
      actions.replaceModeInsertText(character);
      if (lastChange != null) {
        lastChange.append(character);
      }
      newSequence = remainder;
    }

    return newSequence;
  }

  private String evaluateInsertSequence() {
    String newSequence = sequence;
    if (sequence.matches("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>|<LEFT>|<RIGHT>).*")) {
      Matcher matcher = Pattern.compile("^(<ESC>|<BACKSPACE>|<DEL>|<TAB>|<S-TAB>|<ENTER>|<LEFT>|<RIGHT>)(.*)").matcher(sequence);
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
          if (lastChange != null) {
            lastChange.append(key);
          }
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
        case "<LEFT>":
          actions.insertModeBackwardChar(1);
          if (lastChange != null) {
            lastChange.arrowKeyReset();
          }
          break;
        case "<RIGHT>":
          actions.insertModeForwardChar(1);
          if (lastChange != null) {
            lastChange.arrowKeyReset();
          }
          break;
        case "<DEL>":
          actions.insertModeDelete();
          Log.log("Delete evaluated");
          if (lastChange != null) {
            lastChange.append(key);
          }
          break;
      }
      newSequence = remainder;
    } else {
      String character = sequence.substring(0, 1);
      String remainder = sequence.substring(1);
      actions.insertModeInsertText(character);
      if (lastChange != null) {
        lastChange.append(character);
      }
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
        case "<DEL>":
        case "<BACKSPACE>":
          boolean canceled = actions.searchModeBackspace();
          if (canceled) {
            pendingLastChange = null;
          } else if (pendingLastChange != null) {
            pendingLastChange.deleteLastKey();
          }
          break;
        case "<ENTER>":
          actions.searchModeExecuteSearch();
          if (pendingLastChange != null) {
            pendingLastChange.append(key);
            lastChange = pendingLastChange;
            pendingLastChange = null;
          }
          break;
        case "<TAB>":
        case "<S-TAB>":
        case "<ESC>":
          actions.searchModeFinalizeSearch(false);
          pendingLastChange = null;
          break;
      }
      newSequence = remainder;
    } else {
      String character = sequence.substring(0, 1);
      String remainder = sequence.substring(1);
      actions.searchModeAddChar(character);
      if (pendingLastChange != null) {
        pendingLastChange.append(character);
      }
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
