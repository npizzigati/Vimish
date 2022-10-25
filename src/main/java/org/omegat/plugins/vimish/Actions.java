package org.omegat.plugins.vimish;

import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.IEditor.CaretPosition;
import org.omegat.util.Log;

import java.awt.Container;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

class Actions {
  private EditorController editor;
  private Pattern beginningOfWordPattern = Pattern.compile("(^[\\p{L}\\d\\p{P}])|((?<=[^\\p{L}\\d])[\\p{L}\\d])|(?<=[\\p{L}\\d\\s])([^\\p{L}\\d\\s])|(.$)");
  private Pattern beginningOfWordBigWPattern = Pattern.compile("((?<=[\\s])[\\p{L}\\d\\p{P}])|(.$)");
  private Pattern beginningOfWordBackPattern = Pattern.compile("(^[\\p{L}\\d\\p{P}])|((?<=[^\\p{L}\\d])[\\p{L}\\d])|(?<=[\\p{L}\\d\\s])([^\\p{L}\\d\\s])");
  private Pattern beginningOfWordBackBigBPattern = Pattern.compile("((?<=[\\s])[\\p{L}\\d\\p{P}])|(^.)");
  private Pattern endOfWordPattern = Pattern.compile("([\\p{L}\\d](?=[^\\p{L}\\d]))|([^\\p{L}\\d\\s])(?=[\\p{L}\\d\\s])|(.$)");
  private Pattern endOfWordBigEPattern = Pattern.compile("([\\p{L}\\d\\p{P}](?=[\\s]))|(.$)");
  private enum MotionType {
    TO_OR_TILL,
    FORWARD_WORD,
    FORWARD_CHAR
  }
  private enum ObjectType {
    WORD,
    WSPACE,
    PUNCT,
    OTHER
  }

  Container displayFrame;

  Actions(EditorController editor) {
      this.editor = editor;
  }

  class EndIndexResult {
    int endIndex;
    boolean isEndIndexExpanded;

    EndIndexResult(int endIndex, boolean isEndIndexExpanded) {
      this.endIndex = endIndex;
      this.isEndIndexExpanded = isEndIndexExpanded;
    }
  }

  void undo() {
    editor.undo();
    // Move caret back one if it ends up one past last index (on
    // segment end marker)
    int caretIndex = getCaretIndex();
    if (caretIndex == editor.getCurrentTranslation().length()) {
      Log.log("moving index back in forward char");
      setCaretIndex(caretIndex - 1);
    }
  }

  void clearVisualMarks() {
    VimishVisualMarker.resetMarks();
    editor.remarkOneMarker(VimishVisualMarker.class.getName());
  }

  void visualModeYank() {
    Integer startIndex = VimishVisualMarker.getMarkStart();
    Integer endIndex = VimishVisualMarker.getMarkEnd();
    String currentTranslation = editor.getCurrentTranslation();
    String yankedText = currentTranslation.substring(startIndex, endIndex);
    Registers registers = Registers.getRegisters();

    registers.storeYankedText(yankedText);
    setCaretIndex(startIndex);
  }

  void visualModeDelete() {
    // Delete all visually selected text
    Integer startIndex = VimishVisualMarker.getMarkStart();
    Integer endIndex = VimishVisualMarker.getMarkEnd();
    String currentTranslation = editor.getCurrentTranslation();
    String deletedText = currentTranslation.substring(startIndex, endIndex);

    Registers registers = Registers.getRegisters();
    // If text is less than one line, store it in "small delete" register
    // (currentTranslation is assumed to be a maximum of 1 line long)
    if (currentTranslation.equals(deletedText)) {
      registers.storeBigDeletion(deletedText);
    } else {
      registers.storeSmallDeletion(deletedText);
    }

    // Delete text
    editor.replacePartOfText("", startIndex, endIndex);

    // If caret ends up on segment marker that is beyond last
    // index, move it back one
    int indexAfterDeletion = getCaretIndex();
    if (indexAfterDeletion == editor.getCurrentTranslation().length()) {
      setCaretIndex(indexAfterDeletion - 1);
    }
  }

  void visualModeBackwardChar(int count) {
    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();
    String markOrientation = VimishVisualMarker.getMarkOrientation();
    int currentIndex;
    if (markOrientation == "leftOfCaret") {
      currentIndex = markStart;
    } else {
      currentIndex = markEnd - 1;
    }

    // Do nothing if currentIndex is 0
    if (currentIndex == 0) {
      return;
    }

    int newIndex = (currentIndex > count) ? currentIndex - count : 0;
    Log.log("newIndex: " + newIndex);
    if (markStart <= newIndex) {
      VimishVisualMarker.setMarkEnd(newIndex + 1);
    } else {
      if (currentIndex == markEnd - 1) {
        // If passing back over original start, set new markEnd to
        // current markStart (subtract 1 from markEnd since visual
        // mark is implemented up to but not including end index)
        VimishVisualMarker.setMarkEnd(markStart + 1);
        // Also set mark orientation
        VimishVisualMarker.setMarkOrientation("leftOfCaret");
      }

      VimishVisualMarker.setMarkStart(newIndex);
    }

    editor.remarkOneMarker(VimishVisualMarker.class.getName());
    setCaretIndex(newIndex);
  }

  void visualModeForwardChar(int count) {
    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();
    String markOrientation = VimishVisualMarker.getMarkOrientation();
    int currentIndex;
    if (markOrientation == null) {
      // If there is no visually marked text yet, the first mark will
      // always be in the forward direction
      VimishVisualMarker.setMarkOrientation("rightOfCaret");
      currentIndex = getCaretIndex();
    } else if (markOrientation == "leftOfCaret") {
      currentIndex = markStart;
    } else {
      currentIndex = markEnd - 1;
    }

    int length = editor.getCurrentTranslation().length();

    // Do nothing if already at last index
    if (currentIndex == length - 1) {
      return;
    }

    int newIndex = (length - currentIndex >= count) ? currentIndex + count : length - 1;

    // If no mark yet
    if (markEnd == null) {
      VimishVisualMarker.setMarkStart(currentIndex);
      VimishVisualMarker.setMarkEnd(newIndex + 1);
    } else if (newIndex < markEnd - 1) {
      VimishVisualMarker.setMarkStart(newIndex);
    } else {
      if (currentIndex == markStart && markEnd - markStart > 1) {
        // If passing back over original start, set new markStart to
        // current markEnd (subtract 1 from markEnd since visual
        // mark is implemented up to but not including end index)
        VimishVisualMarker.setMarkStart(markEnd - 1);
        // Also set mark orientation
        VimishVisualMarker.setMarkOrientation("rightOfCaret");
      }

      VimishVisualMarker.setMarkEnd(newIndex + 1);
    }

    editor.remarkOneMarker(VimishVisualMarker.class.getName());
    setCaretIndex(newIndex);
  }

  void normalModePutSpecificRegister(String registerKey, String position) {
    Registers registers = Registers.getRegisters();
    String text = registers.retrieve(registerKey);
    normalModePut(text, position);
  }

  void normalModePutUnnamedRegister(String position) {
    Registers registers = Registers.getRegisters();
    String text = registers.retrieve("unnamed");
    normalModePut(text, position);
  }

  void normalModePut(String text, String position) {
    int index = getCaretIndex();
    if (position.equals("before")) {
      insertTextAtIndex(text, index);
    } else {
      insertTextAtIndex(text, index + 1);
    }
    normalModeBackwardChar(1);
  }

  void normalModeAppendAfterCursor() {
    int currentIndex = getCaretIndex();
    setCaretIndex(currentIndex + 1);
  }

  ObjectType getObjectType(int currentIndex, String currentTranslation) {
    ObjectType objectType = ObjectType.WORD;
    String currentChar = currentTranslation.substring(currentIndex, currentIndex + 1);
    if (currentChar.matches("\\p{P}")) {
      objectType = ObjectType.PUNCT;
    } else if (currentChar.matches("\\s")) {
      objectType = ObjectType.WSPACE;
    }
    return objectType;
  }

  int getObjectStartIndex(int currentIndex, String currentTranslation, ObjectType objectType, String selector, String delimiter, boolean isEndIndexExpanded) {
    int startIndex = currentIndex;
    String wordBoundaryRegex;
    String punctBoundaryRegex;
    String whitespaceBoundaryRegex;
    String startDelimiter = delimiter;

    if (delimiter.equals("W")) {
      wordBoundaryRegex = "[\\s]";
      punctBoundaryRegex = "[\\s]";
      whitespaceBoundaryRegex = "[\\p{P}\\p{L}]";
    } else {
      wordBoundaryRegex = "[\\p{P}\\s]";
      punctBoundaryRegex = "[\\p{L}\\s]";
      whitespaceBoundaryRegex = "[\\p{P}\\p{L}]";
    }

    if (!delimiter.toLowerCase().equals("w")) {
      switch (delimiter) {
        case ")":
          startDelimiter = "(";
          break;
        case "]":
          startDelimiter = "[";
          break;
        case "}":
          startDelimiter = "{";
          break;
        case ">":
          startDelimiter = "<";
          break;
      }
    }

    loop: while (startIndex > 0) {
      switch (objectType) {
        case WORD:
          if (currentTranslation.substring(startIndex - 1, startIndex).matches(wordBoundaryRegex)) {
            break loop;
          }
          break;
        case PUNCT:
          if (currentTranslation.substring(startIndex - 1, startIndex).matches(punctBoundaryRegex)) {
            break loop;
          }
          break;
        case WSPACE:
          if (currentTranslation.substring(startIndex - 1, startIndex).matches(whitespaceBoundaryRegex)) {
            break loop;
          }
          break;
        case OTHER:
          if (currentTranslation.substring(startIndex - 1, startIndex).equals(startDelimiter)) {
            break loop;
          }
          break;
      }

      startIndex--;
    }

    if (selector.equals("i")) {
      return startIndex;
    }

    // The code below will only be run if the selector is not "i"
    // (i.e., it is "a")

    // Guard against position being extended behind first index
    if (startIndex == 0) {
      return startIndex;
    }

    // If not a word object, endIndex will only be expanded one
    // position (to include delimiter)
    if (!delimiter.toLowerCase().equals("w")) {
      return startIndex - 1;
    }

    // For word objects, do not expand word objects if end index was expanded
    if (isEndIndexExpanded) {
      return startIndex;
    }

    // In the case of a whitespace object, expand through word
    // object behind it
    if (objectType == ObjectType.WSPACE) {
      ObjectType objectTypeUnderIndex = getObjectType(startIndex - 1, currentTranslation);
      return getObjectStartIndex(startIndex - 1, currentTranslation, objectTypeUnderIndex, "i", "w", false);
    }

    // For word and punct object type, if next index ahead is a
    // whitespace, expand through end of whitespace
    ObjectType objectTypeUnderIndex = getObjectType(startIndex - 1, currentTranslation);
    if (objectTypeUnderIndex == ObjectType.WSPACE) {
      return getObjectStartIndex(startIndex, currentTranslation, objectTypeUnderIndex, "i", "w", false);
    } else {
      return startIndex;
    }

  }

  EndIndexResult getObjectEndIndex(int currentIndex, String currentTranslation, ObjectType objectType, String selector, String delimiter) {
    int endIndex = currentIndex;
    String wordBoundaryRegex;
    String punctBoundaryRegex;
    String whitespaceBoundaryRegex;
    String endDelimiter = delimiter;

    if (delimiter.equals("W")) {
      wordBoundaryRegex = "[\\s]";
      punctBoundaryRegex = "[\\s]";
      whitespaceBoundaryRegex = "[\\p{P}\\p{L}]";
    } else {
      wordBoundaryRegex = "[\\p{P}\\s]";
      punctBoundaryRegex = "[\\p{L}\\s]";
      whitespaceBoundaryRegex = "[\\p{P}\\p{L}]";
    }

    if (!delimiter.toLowerCase().equals("w")) {
      switch (delimiter) {
        case "(":
          endDelimiter = ")";
          break;
        case "[":
          endDelimiter = "]";
          break;
        case "{":
          endDelimiter = "}";
          break;
        case "<":
          endDelimiter = ">";
          break;
      }
    }

    loop: while (endIndex < currentTranslation.length() - 1) {
      switch (objectType) {
      case WORD:
        if (currentTranslation.substring(endIndex + 1, endIndex + 2).matches(wordBoundaryRegex)) {
          break loop;
        }
        break;
      case PUNCT:
        if (currentTranslation.substring(endIndex + 1, endIndex + 2).matches(punctBoundaryRegex)) {
          break loop;
        }
        break;
      case WSPACE:
        if (currentTranslation.substring(endIndex + 1, endIndex + 2).matches(whitespaceBoundaryRegex)) {
          break loop;
        }
        break;
      case OTHER:
        if (currentTranslation.substring(endIndex + 1, endIndex + 2).equals(endDelimiter)) {
          break loop;
        }
        break;
      }

      endIndex++;
    }

    if (selector.equals("i")) {
      return new EndIndexResult(endIndex, false);
    }

    // The code below will only be run if the selector is not "i"
    // (i.e., it is "a")

    // Guard against position being extended beyond last index
    if (endIndex == currentTranslation.length() - 1) {
      return new EndIndexResult(endIndex, false);
    }

    // If not a word object, endIndex will only be expanded one
    // position (to include delimiter)
    if (!delimiter.toLowerCase().equals("w")) {
      return new EndIndexResult(endIndex + 1, true);
    }

    // In the case of a whitespace object, expand through word
    // object ahead of it
    if (objectType == ObjectType.WSPACE) {
      ObjectType objectTypeUnderIndex = getObjectType(endIndex + 1, currentTranslation);
      int newEndIndex = getObjectEndIndex(endIndex + 1, currentTranslation, objectTypeUnderIndex, "i", "w").endIndex;
      return new EndIndexResult(newEndIndex, (endIndex == newEndIndex) ? false : true);
    }

    // For word and punct object type, if next index ahead is a
    // whitespace, expand through end of whitespace
    ObjectType objectTypeUnderIndex = getObjectType(endIndex + 1, currentTranslation);
    if (objectTypeUnderIndex == ObjectType.WSPACE) {
      int newEndIndex = getObjectEndIndex(endIndex, currentTranslation, objectTypeUnderIndex, "i", "w").endIndex;
      return new EndIndexResult(newEndIndex, (endIndex == newEndIndex) ? false : true);
    } else {
      return new EndIndexResult(endIndex, false);
    }
  }

  void normalModeTextObjectSelection(String operator, String selector, String delimiter) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int endIndex = currentIndex;
    int startIndex = currentIndex;

    ObjectType objectType;
    if (delimiter.toLowerCase().equals("w")) {
      objectType = getObjectType(currentIndex, currentTranslation);
    } else {
      objectType = ObjectType.OTHER;
    }
    EndIndexResult endIndexResult = getObjectEndIndex(currentIndex, currentTranslation, objectType, selector, delimiter);
    endIndex = endIndexResult.endIndex;
    boolean isEndIndexExpanded = endIndexResult.isEndIndexExpanded;
    startIndex = getObjectStartIndex(currentIndex, currentTranslation, objectType, selector, delimiter, isEndIndexExpanded);
    setCaretIndex(startIndex);
    switch (operator) {
      case "y":
        executeForwardAction("y", MotionType.FORWARD_WORD, currentTranslation, startIndex, endIndex + 1);
        break;
      case "d":
        executeForwardAction("d", MotionType.FORWARD_WORD, currentTranslation, startIndex, endIndex + 1);
        break;
      case "c":
        executeForwardAction("c", MotionType.FORWARD_WORD, currentTranslation, startIndex, endIndex + 1);
        break;
    }
  }

  void normalModeGoBackwardToChar(int count, String operator, String motion, String key) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int newIndex = currentIndex;
    int iterations = 0;
    while (iterations < count) {
      iterations++;
      newIndex = currentTranslation.lastIndexOf(key, newIndex - 1);
      if (newIndex == -1) {
        return;
      }
    }
    // If motion is "til" ("t"), final index should land on index
    // before character
    if (motion.equals("T")) {
      newIndex += 1;
    }
    executeBackwardAction(operator, currentTranslation, currentIndex, newIndex);
  }

  void normalModeGoForwardToChar(int count, String operator, String motion, String key) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int newIndex = currentIndex;
    int iterations = 0;
    while (iterations < count) {
      iterations++;
      newIndex = currentTranslation.indexOf(key, newIndex + 1);
      if (newIndex == -1) {
        return;
      }
    }
    // If motion is "til" ("t"), final index should land on index
    // before character
    if (motion.equals("t")) {
      newIndex -= 1;
    }
    executeForwardAction(operator, MotionType.TO_OR_TILL, currentTranslation, currentIndex, newIndex + 1);
  }

  void normalModeForwardWord(String operator, String motion, int count) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int length = currentTranslation.length();
    Pattern pattern = beginningOfWordPattern;
    switch (motion) {
    case "W":
      pattern = beginningOfWordBigWPattern;
      break;
    case "e":
      pattern = endOfWordPattern;
      break;
    case "E":
      pattern = endOfWordBigEPattern;
      break;
    }
    String textToEnd = currentTranslation.substring(currentIndex, length);
    Matcher m = pattern.matcher(textToEnd);
    int iterations = 1;
    int newIndex = currentIndex;
    while (iterations <= count && m.find()) {
      // If current index is on a match, we move to the next match
      if (m.start() == 0) {
        continue;
      }
      iterations++;
      newIndex = currentIndex + m.start();
    }

    // If this is a "w" motion and new index lands on last
    // character in segment and there is an operator, we need to
    // change the algorithm we use to get the new index, to
    // assure the selection endpoint is correct
    if (!operator.equals("") && newIndex == length - 1 && motion.toLowerCase().equals("w")) {
      String newMotion = (motion.equals("w") ? "e" : "E");
      normalModeForwardWord(operator, newMotion, count);
      return;
    }

    // For d/c/y operations with the "e" motion, we need to
    // increment the new index by one to ensure selection is correct
    if (!operator.equals("") && motion.toLowerCase().equals("e")) {
      newIndex++;
    }

    executeForwardAction(operator, MotionType.FORWARD_WORD, currentTranslation, currentIndex, newIndex);
  }

  void executeForwardAction(String operator, MotionType motionType, String currentTranslation,
                            int currentIndex, int newIndex) {
    int length = currentTranslation.length();
    if (operator.equals("")) {
      setCaretIndex((motionType == MotionType.TO_OR_TILL) ? newIndex - 1 : newIndex);
    } else {
      String yankedText = currentTranslation.substring(currentIndex, newIndex);
      Registers registers = Registers.getRegisters();
      // If text is less than one line, store it in "small delete" register
      // (currentTranslation is assumed to be a maximum of 1 line long)
      if (currentTranslation.equals(yankedText)) {
        registers.storeBigDeletion(yankedText);
      } else {
        registers.storeSmallDeletion(yankedText);
      }

      if (operator.equals("d")) {
        Log.log("deleting word, newIndex: " + newIndex);
        editor.replacePartOfText("", currentIndex, newIndex);
      } else if (operator.equals("c")) {
        // If this is a word operation and new index falls on a
        // word char (but not at end of segment), back off
        // newIndex one position (new index in that case should
        // fall one before the beginning ot the next word)
        Log.log("changing word, newIndex: " + newIndex);
        if (motionType == MotionType.FORWARD_WORD && newIndex != length && currentTranslation.substring(newIndex, newIndex + 1).matches("\\p{L}")) {
          editor.replacePartOfText("", currentIndex, newIndex - 1);
        } else {
          editor.replacePartOfText("", currentIndex, newIndex);
        }
        Mode.INSERT.activate();
      }
    }

    // Move caret back one if it ends up one past last index (on
    // segment end marker)
    if (newIndex == length) {
      Log.log("moving index back in forward char");
      setCaretIndex(getCaretIndex() - 1);
    }
  }

  void executeBackwardAction(String operator, String currentTranslation, int currentIndex, int newIndex) {
    if (operator.equals("")) {
      setCaretIndex(newIndex);
    } else {
      String yankedText = currentTranslation.substring(newIndex, currentIndex);
      Registers registers = Registers.getRegisters();
      // If text is less than one line, store it in "small delete" register
      // (currentTranslation is assumed to be a maximum of 1 line long)
      if (currentTranslation.equals(yankedText)) {
        registers.storeBigDeletion(yankedText);
      } else {
        registers.storeSmallDeletion(yankedText);
      }

      if (operator.equals("d")) {
        editor.replacePartOfText("", newIndex, currentIndex);
      } else if (operator.equals("c")) {
        editor.replacePartOfText("", newIndex, currentIndex);
        Mode.INSERT.activate();
      }
    }
  }

  void normalModeForwardChar(String operator, int count) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int length = currentTranslation.length();
    int newIndex = (length - currentIndex >= count) ? currentIndex + count : length;
    executeForwardAction(operator, MotionType.FORWARD_CHAR, currentTranslation, currentIndex, newIndex);
  }

  void normalModeBackwardWord(String operator, String motion, int totalCount) {
    int currentIndex = getCaretIndex();
    if (currentIndex == 0) {
      return;
    }
    String currentTranslation = editor.getCurrentTranslation();
    Pattern pattern = beginningOfWordBackPattern;
    if (motion.equals("B")) {
      pattern = beginningOfWordBackBigBPattern;
    }
    int newIndex = currentIndex;
    String textFromStart = currentTranslation.substring(0, currentIndex);
    Matcher m = pattern.matcher(textFromStart);
    ArrayList<Integer> allMatchIndexes = new ArrayList<>();
    while (m.find()) {
      allMatchIndexes.add(m.start());
    }
    int matchPosition = allMatchIndexes.size() - totalCount;
    if (matchPosition < 0) {
      matchPosition = 0;
    }
    newIndex = allMatchIndexes.isEmpty() ? currentIndex : allMatchIndexes.get(matchPosition);
    executeBackwardAction(operator, currentTranslation, currentIndex, newIndex);
  }

  void normalModeBackwardChar(int count) {
    normalModeBackwardChar("", count);
  }

  void normalModeBackwardChar(String operator, int count) {
    int currentIndex = getCaretIndex();
    int newIndex = (currentIndex >= count) ? currentIndex - count : 0;
    String currentTranslation = editor.getCurrentTranslation();
    executeBackwardAction(operator, currentTranslation, currentIndex, newIndex);
  }

  void normalModeTab() {
    if (editor.getSettings().isUseTabForAdvance() == true) {
      editor.nextEntry();
    }
  }

  void normalModeShiftTab() {
    if (editor.getSettings().isUseTabForAdvance() == true) {
      editor.prevEntry();
    }
  }

  void insertModeInsertText(String text) {
    editor.insertText(text);
  }

  void insertModeBackspace() {
    int currentIndex = getCaretIndex();
    if (currentIndex == 0) {
      return;
    }
    editor.replacePartOfText("", currentIndex - 1, currentIndex);
  }

  void insertModeDelete() {
    int currentIndex = getCaretIndex();
    int length = editor.getCurrentTranslation().length();
    if (currentIndex == length) {
      return;
    }
    editor.replacePartOfText("", currentIndex, currentIndex + 1);
  }

  void insertModeEnter() {
    editor.insertText("\n");
  }

  void insertModeTab() {
    editor.insertText("\t");
  }

  private void insertTextAtIndex(String text, int index) {
    setCaretIndex(index);
    editor.insertText(text);
  }

  boolean isCaretPastLastIndex() {
    int currentIndex = getCaretIndex();
    int length = editor.getCurrentTranslation().length();

    return currentIndex == length;
  }

  /**
   * Set caret position through OmegaT API
   */
  void setCaretIndex(int index) {
    CaretPosition newCaretPosition = new CaretPosition(index);
      editor.setCaretPosition(newCaretPosition);
  }

  /**
   * Get caret position from OmegaT API
   */
  int getCaretIndex() {
    return editor.getCurrentPositionInEntryTranslation();
  }
}
