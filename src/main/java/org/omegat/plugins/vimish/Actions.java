package org.omegat.plugins.vimish;

import org.omegat.core.Core;
import org.omegat.gui.main.MainWindow;
import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.IEditor.CaretPosition;
import org.omegat.util.Log;

import java.awt.Container;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.Character;

class Actions {
  private EditorController editor;
  private MainWindow mainWindow;
  // private JFrame searchPopup;
  // private JLabel searchPopupLabel;
  private String searchString;
  private String searchOperator;
  private Mode preSearchMode;
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
    this.mainWindow = (MainWindow) Core.getMainWindow();
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

  void beginSingleCharVisualSelection() {
    int currentIndex = getCaretIndex();
    VimishVisualMarker.setMarkStart(currentIndex);
    VimishVisualMarker.setMarkEnd(currentIndex + 1);
    VimishVisualMarker.setMarkOrientation("rightOfCaret");
    editor.remarkOneMarker(VimishVisualMarker.class.getName());
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

  void visualModeBackwardMove(int currentIndex, int newIndex) {
    if (currentIndex == newIndex) {
      return;
    }
    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();

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

  void visualModeBackwardChar(int count) {
    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();
    String markOrientation = VimishVisualMarker.getMarkOrientation();
    int currentIndex;
    if (markOrientation.equals("leftOfCaret")) {
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
    visualModeBackwardMove(currentIndex, newIndex);
  }

  void visualModeForwardChar(int count) {
    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();
    String markOrientation = VimishVisualMarker.getMarkOrientation();
    int currentIndex;

    if (markOrientation.equals("leftOfCaret")) {
      currentIndex = markStart;
    } else {
      currentIndex = markEnd - 1;
    }
    // if (markOrientation == null) {
    //   // If there is no visually marked text yet, the first mark will
    //   // always be in the forward direction
    //   VimishVisualMarker.setMarkOrientation("rightOfCaret");
    //   currentIndex = getCaretIndex();
    // } else if (markOrientation == "leftOfCaret") {
    //   currentIndex = markStart;
    // } else {
    //   currentIndex = markEnd - 1;
    // }

    int length = editor.getCurrentTranslation().length();

    // Do nothing if already at last index
    if (currentIndex == length - 1) {
      return;
    }

    int newIndex = (length - currentIndex >= count) ? currentIndex + count : length - 1;

    visualModeForwardMove(currentIndex, newIndex);
  }

  void visualModeBackwardWord(String motion, int totalCount) {
    int currentIndex = getCaretIndex();
    if (currentIndex == 0) {
      return;
    }
    String currentTranslation = editor.getCurrentTranslation();
    int newIndex = getBackwardWordIndex(currentIndex, totalCount, motion, currentTranslation);
    visualModeBackwardMove(currentIndex, newIndex);
  }

  void visualModeForwardWord(String motion, int count) {
    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();
    String markOrientation = VimishVisualMarker.getMarkOrientation();
    int currentIndex;
    if (markOrientation == null) {
      // If there is no visually marked text yet, the first mark will
      // always be in the forward direction
      VimishVisualMarker.setMarkOrientation("rightOfCaret");
      currentIndex = getCaretIndex();
    } else if (markOrientation.equals("leftOfCaret")) {
      currentIndex = markStart;
    } else {
      currentIndex = markEnd - 1;
    }
    String currentTranslation = editor.getCurrentTranslation();
    int newIndex = getForwardWordIndex(currentIndex, motion, count, currentTranslation);
    visualModeForwardMove(currentIndex, newIndex);
  }

  void visualModeForwardMove(int currentIndex, int newIndex) {
    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();
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

  void visualModeReplace(String key) {
    Integer startIndex = VimishVisualMarker.getMarkStart();
    Integer endIndex = VimishVisualMarker.getMarkEnd();
    int count = endIndex - startIndex;
    editor.replacePartOfText(repeat(key, count), startIndex, endIndex);
    setCaretIndex(startIndex);
  }

  void visualModeBigDCY(String operator) {
    String currentTranslation = editor.getCurrentTranslation();
    switch (operator) {
    case "D":
      Mode.NORMAL.activate();
      clearVisualMarks();
      executeForwardAction("d", MotionType.FORWARD_CHAR, currentTranslation, 0, currentTranslation.length());
      break;
    case "C":
      Mode.NORMAL.activate();
      clearVisualMarks();
      executeForwardAction("d", MotionType.FORWARD_CHAR, currentTranslation, 0, currentTranslation.length());
      Mode.INSERT.activate();
      break;
    case "Y":
      clearVisualMarks();
      Mode.NORMAL.activate();
      executeForwardAction("y", MotionType.FORWARD_CHAR, currentTranslation, 0, currentTranslation.length());
      setCaretIndex(0);
      break;
    }
  }


  void normalModeReplace(String key, int count) {
    String currentTranslation = editor.getCurrentTranslation();
    int lastIndex = currentTranslation.length() - 1;
    int currentIndex = getCaretIndex();
    // If count extends beyond end of segment, do nothing
    if (currentIndex + count > lastIndex + 1) {
      return;
    }
    editor.replacePartOfText(repeat(key, count), currentIndex, currentIndex + count);
    int indexAfterOverwrite = getCaretIndex();
    setCaretIndex(indexAfterOverwrite - 1);
  }

  void normalModeBigDCY(String operator) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    switch (operator) {
    case "D":
      executeForwardAction("d", MotionType.FORWARD_CHAR, currentTranslation, currentIndex, currentTranslation.length());
      break;
    case "C":
      executeForwardAction("d", MotionType.FORWARD_CHAR, currentTranslation, currentIndex, currentTranslation.length());
      setCaretIndex(getCaretIndex() + 1);
      Mode.INSERT.activate();
      break;
    case "Y":
      executeForwardAction("y", MotionType.FORWARD_CHAR, currentTranslation, 0, currentTranslation.length());
      break;
    }
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

  void normalModeActivateSearch(String operator, Mode activatingMode) {
    preSearchMode = activatingMode;
    searchOperator = operator;
    searchString = "";
    mainWindow.showStatusMessageRB("PREFERENCES_SEARCH_HINT", searchOperator);
  }

  void searchModeFinalizeSearch(boolean resetSearch) {
    mainWindow.showStatusMessageRB(null);
    preSearchMode.activate();
    if (resetSearch) {
      searchString = "";
    }
  }

  void searchModeBackspace() {
    if (searchString.equals("")) {
      mainWindow.showStatusMessageRB(null);
      searchModeFinalizeSearch(true);
    }
    searchString = searchString.substring(0, searchString.length() - 1);
    mainWindow.showStatusMessageRB("PREFERENCES_SEARCH_HINT", searchOperator + searchString);
  }

  void searchModeAddChar(String character) {
    searchString += character;
    mainWindow.showStatusMessageRB("PREFERENCES_SEARCH_HINT", searchOperator + searchString);
  }

  void searchModeExecuteSearch() {
    if (searchOperator.equals("/")) {
      searchModeForwardSearch();
    } else {
      searchModeBackwardSearch();
    }
  }

  void searchModeForwardSearch() {
    searchModeFinalizeSearch(false);
    if (searchString == null || searchString.equals("")) {
      return;
    }
    String currentTranslation = editor.getCurrentTranslation();
    int currentIndex = getCaretIndex();
    if (currentIndex >= currentTranslation.length() - 1) {
      return;
    }
    int newIndex = getForwardSearchIndex(currentTranslation, currentIndex);
    if (newIndex == currentIndex) {
      return;
    }
    if (Mode.NORMAL.isActive()) {
      setCaretIndex(newIndex);
    } else {
      visualModeForwardMove(currentIndex, newIndex);
    }
  }

  void repeatForwardSearch(int count, String operator) {
    if (searchString == null || searchString.equals("")) {
      return;
    }
    String currentTranslation = editor.getCurrentTranslation();
    int currentIndex = getCaretIndex();
    int tmpIndex = currentIndex;
    int newIndex = currentIndex;
    int iterations = 0;
    while (iterations < count) {
      iterations++;
      newIndex = getForwardSearchIndex(currentTranslation, tmpIndex);
      if (newIndex == tmpIndex) {
        break;
      }
      tmpIndex = newIndex;
    }

    if (Mode.NORMAL.isActive()) {
      executeForwardAction(operator, MotionType.FORWARD_CHAR, currentTranslation, currentIndex, newIndex);
    } else {
      visualModeForwardMove(currentIndex, newIndex);
    }
  }

  void repeatBackwardSearch(int count, String operator) {
    if (searchString == null || searchString.equals("")) {
      return;
    }
    String currentTranslation = editor.getCurrentTranslation();
    int currentIndex = getCaretIndex();
    int tmpIndex = currentIndex;
    int newIndex = getBackwardSearchIndex(currentTranslation, currentIndex);
    int iterations = 0;
    while (iterations < count) {
      iterations++;
      newIndex = getBackwardSearchIndex(currentTranslation, tmpIndex);
      if (newIndex == tmpIndex) {
        break;
      }
      tmpIndex = newIndex;
    }
    if (Mode.NORMAL.isActive()) {
      executeBackwardAction(operator, currentTranslation, currentIndex, newIndex);
    } else {
      visualModeBackwardMove(currentIndex, newIndex);
    }
  }

  void searchModeBackwardSearch() {
    searchModeFinalizeSearch(false);
    if (searchString == null || searchString.equals("")) {
      return;
    }
    String currentTranslation = editor.getCurrentTranslation();
    int currentIndex = getCaretIndex();
    if (currentIndex == 0) {
      return;
    }
    int newIndex = getBackwardSearchIndex(currentTranslation, currentIndex);
    if (newIndex == currentIndex) {
      return;
    }
    if (Mode.NORMAL.isActive()) {
      setCaretIndex(newIndex);
    } else {
      visualModeBackwardMove(currentIndex, newIndex);
    }
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

  int getObjectStartIndex(int currentIndex, String currentTranslation, ObjectType objectType, String selector,
      String delimiter, boolean isEndIndexExpanded) {
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

    // For word objects, do not expand start index if end index was expanded
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

  EndIndexResult getObjectEndIndex(int currentIndex, String currentTranslation, ObjectType objectType, String selector,
      String delimiter) {
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

  void visualModeTextObjectSelection(String selector, String delimiter) {
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
    EndIndexResult endIndexResult = getObjectEndIndex(currentIndex, currentTranslation, objectType, selector,
        delimiter);
    endIndex = endIndexResult.endIndex;
    boolean isEndIndexExpanded = endIndexResult.isEndIndexExpanded;
    startIndex = getObjectStartIndex(currentIndex, currentTranslation, objectType, selector, delimiter,
        isEndIndexExpanded);

    String markOrientation = VimishVisualMarker.getMarkOrientation();
    boolean isSelectionSingleChar = VimishVisualMarker.getMarkStart() + 1 == VimishVisualMarker.getMarkEnd();
    if (isSelectionSingleChar) {
      // If there is no visually marked text before selection, mark will
      // be in the forward direction
      VimishVisualMarker.setMarkStart(startIndex);
      VimishVisualMarker.setMarkEnd(endIndex + 1);
      editor.remarkOneMarker(VimishVisualMarker.class.getName());
      VimishVisualMarker.setMarkOrientation("rightOfCaret");
      setCaretIndex(endIndex);
    } else if (markOrientation.equals("leftOfCaret")){
      VimishVisualMarker.setMarkStart(startIndex);
      editor.remarkOneMarker(VimishVisualMarker.class.getName());
      setCaretIndex(startIndex);
    } else {
      VimishVisualMarker.setMarkEnd(endIndex + 1);
      editor.remarkOneMarker(VimishVisualMarker.class.getName());
      setCaretIndex(endIndex);
    }
  }

  void visualModeSwitchCase(String operator) {
    Integer startIndex = VimishVisualMarker.getMarkStart();
    Integer endIndex = VimishVisualMarker.getMarkEnd();
    String currentTranslation = editor.getCurrentTranslation();
    String selection = currentTranslation.substring(startIndex, endIndex);
    if (operator.equals("u")) {
      selection = selection.toLowerCase();
    } else {
      selection = selection.toUpperCase();
    }
    editor.replacePartOfText(selection, startIndex, endIndex);
    // Replacement operation will place caret at end of replaced
    // text -- we need to put it back in proper place
    if (VimishVisualMarker.getMarkOrientation().equals("rightOfCaret")) {
      setCaretIndex(endIndex - 1);
    } else {
      setCaretIndex(startIndex);
    }
  }

  void normalModeToggleCase(int count) {
    String currentTranslation = editor.getCurrentTranslation();
    int startIndex = getCaretIndex();
    int length = currentTranslation.length();
    int endIndex = (startIndex + count > length - 1) ? length - 1 : startIndex + count;
    String toggledString = "";
    for (int i = startIndex; i < endIndex; i++) {
      char character = currentTranslation.charAt(i);
      if (Character.isLowerCase(character)) {
        toggledString += Character.toString(character).toUpperCase();
      } else {
        toggledString += Character.toString(character).toLowerCase();
      }
    }
    editor.replacePartOfText(toggledString, startIndex, endIndex);

    // Move caret back one if it ends up past last index (on
    // segment end marker)
    if (endIndex == currentTranslation.length()) {
      setCaretIndex(getCaretIndex() - 1);
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
    EndIndexResult endIndexResult = getObjectEndIndex(currentIndex, currentTranslation, objectType, selector,
        delimiter);
    endIndex = endIndexResult.endIndex;
    boolean isEndIndexExpanded = endIndexResult.isEndIndexExpanded;
    startIndex = getObjectStartIndex(currentIndex, currentTranslation, objectType, selector, delimiter,
        isEndIndexExpanded);
    setCaretIndex(startIndex);
    executeForwardAction(operator, MotionType.FORWARD_WORD, currentTranslation, startIndex, endIndex + 1);
  }

  int getForwardSearchIndex(String currentTranslation, int currentIndex) {
    String textToEnd = currentTranslation.substring(currentIndex + 1);
    Matcher m = Pattern.compile(searchString)
                            .matcher(textToEnd);
    int matchStart;
    if (m.find()) {
      matchStart = m.start();
    } else {
      return currentIndex;
    }
    int newIndex = currentIndex + 1 + matchStart;
    return newIndex;
  }

  int getBackwardSearchIndex(String currentTranslation, int currentIndex) {
    int newIndex = currentIndex;
    String textFromStart = currentTranslation.substring(0, currentIndex);
    Matcher m = Pattern.compile(searchString)
                            .matcher(textFromStart);
    ArrayList<Integer> allMatchIndexes = new ArrayList<>();
    while (m.find()) {
      allMatchIndexes.add(m.start());
    }
    int totalMatches = allMatchIndexes.size();
    if (totalMatches > 0) {
      newIndex = allMatchIndexes.get(totalMatches - 1);
    }
    return newIndex;
  }

  int getBackwardToCharIndex(int count, int currentIndex, String motion, String key, String currentTranslation) {
    int newIndex = currentIndex;
    int iterations = 0;
    while (iterations < count) {
      iterations++;
      newIndex = currentTranslation.lastIndexOf(key, newIndex - 1);
      if (newIndex == -1) {
        return currentIndex;
      }
    }
    // If motion is "til" ("t"), final index should land on index
    // before character
    if (motion.equals("T")) {
      newIndex += 1;
    }
    return newIndex;
  }

  int getForwardToCharIndex(int count, int currentIndex, String motion, String key, String currentTranslation) {
    int newIndex = currentIndex;
    int iterations = 0;
    while (iterations < count) {
      iterations++;
      newIndex = currentTranslation.indexOf(key, newIndex + 1);
      if (newIndex == -1) {
        return currentIndex;
      }
    }
    // If motion is "til" ("t"), final index should land on index
    // before character
    if (motion.equals("t")) {
      newIndex -= 1;
    }
    return newIndex;
  }

  void visualModeGoForwardToChar(int count, String motion, String key) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int newIndex = getForwardToCharIndex(count, currentIndex, motion, key, currentTranslation);
    if (newIndex == currentIndex) {
      return;
    }
    visualModeForwardMove(currentIndex, newIndex);
  }

  void visualModeGoBackwardToChar(int count, String motion, String key) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int newIndex = getBackwardToCharIndex(count, currentIndex, motion, key, currentTranslation);
    if (newIndex == currentIndex) {
      return;
    }
    visualModeBackwardMove(currentIndex, newIndex);
  }

  void normalModeGoForwardToChar(int count, String operator, String motion, String key) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int newIndex = getForwardToCharIndex(count, currentIndex, motion, key, currentTranslation);
    if (newIndex == currentIndex) {
      return;
    }
    executeForwardAction(operator, MotionType.TO_OR_TILL, currentTranslation, currentIndex, newIndex + 1);
  }

  void normalModeGoBackwardToChar(int count, String operator, String motion, String key) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int newIndex = getBackwardToCharIndex(count, currentIndex, motion, key, currentTranslation);
    if (newIndex == currentIndex) {
      return;
    }
    executeBackwardAction(operator, currentTranslation, currentIndex, newIndex);
  }

  int getForwardWordIndex(int currentIndex, String motion, int count, String currentTranslation) {
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

    return newIndex;
  }

  void normalModeForwardWord(String operator, String motion, int count) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int length = currentTranslation.length();

    int newIndex = getForwardWordIndex(currentIndex, motion, count, currentTranslation);

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
    // segment end marker) and operation is not a yank
    if (!operator.equals("y") && newIndex == length) {
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

  int getBackwardWordIndex(int currentIndex, int count, String motion, String currentTranslation) {
    Pattern pattern = beginningOfWordBackPattern;
    if (motion.equals("B")) {
      pattern = beginningOfWordBackBigBPattern;
    }
    String textFromStart = currentTranslation.substring(0, currentIndex);
    Matcher m = pattern.matcher(textFromStart);
    ArrayList<Integer> allMatchIndexes = new ArrayList<>();
    while (m.find()) {
      allMatchIndexes.add(m.start());
    }
    int matchPosition = allMatchIndexes.size() - count;
    if (matchPosition < 0) {
      matchPosition = 0;
    }
    return allMatchIndexes.isEmpty() ? currentIndex : allMatchIndexes.get(matchPosition);
  }

  void normalModeBackwardWord(String operator, String motion, int totalCount) {
    int currentIndex = getCaretIndex();
    if (currentIndex == 0) {
      return;
    }
    String currentTranslation = editor.getCurrentTranslation();

    int newIndex = getBackwardWordIndex(currentIndex, totalCount, motion, currentTranslation);

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

  void replaceModeInsertText(String text) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int length = currentTranslation.length();
    if (currentIndex < length) {
      editor.replacePartOfText(text, currentIndex, currentIndex + 1);
    } else {
      editor.insertText(text);
    }
  }

  void replaceModeBackspace() {
    int currentIndex = getCaretIndex();
    if (currentIndex == 0) {
      return;
    }
    setCaretIndex(currentIndex - 1);
  }

  void replaceModeDelete() {
    int currentIndex = getCaretIndex();
    int length = editor.getCurrentTranslation().length();
    if (currentIndex == length) {
      return;
    }
    editor.replacePartOfText("", currentIndex, currentIndex + 1);
  }

  void replaceModeEnter() {
    editor.insertText("\n");
  }

  void replaceModeTab() {
    if (editor.getSettings().isUseTabForAdvance() == true) {
      editor.nextEntry();
    } else {
      editor.insertText("\t");
    }
  }

  void replaceModeShiftTab() {
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
    if (editor.getSettings().isUseTabForAdvance() == true) {
      editor.nextEntry();
    } else {
      editor.insertText("\t");
    }
  }

  void insertModeShiftTab() {
    if (editor.getSettings().isUseTabForAdvance() == true) {
      editor.prevEntry();
    }
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

  String repeat(String word, int times) {
    return String.join("", Collections.nCopies(times, word));
  }
}
