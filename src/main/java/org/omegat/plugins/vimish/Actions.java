package org.omegat.plugins.vimish;

import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.IEditor.CaretPosition;
import org.omegat.util.Log;

class Actions {
  private EditorController editor;

  Actions(EditorController editor) {
    this.editor = editor;
  }

  void undo() {
    editor.undo();
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

  void normalModeForwardChar(String operator, int count) {
    int currentIndex = getCaretIndex();
    String currentTranslation = editor.getCurrentTranslation();
    int length = currentTranslation.length();
    int newIndex = (length - currentIndex >= count) ? currentIndex + count : length;
    if (operator.equals("")) {
      setCaretIndex(newIndex);
    } else {

      // TODO: make this work for yanks too (text will be stored
      // in different register). Also need to implement for backward char
      String deletedText = currentTranslation.substring(currentIndex, newIndex);
      Registers registers = Registers.getRegisters();
      // If text is less than one line, store it in "small delete" register
      // (currentTranslation is assumed to be a maximum of 1 line long)
      if (currentTranslation.equals(deletedText)) {
        registers.storeBigDeletion(deletedText);
      } else {
        registers.storeSmallDeletion(deletedText);
      }

      if (operator.equals("d")) {
        editor.replacePartOfText("", currentIndex, newIndex);
      }
    }

    // Move caret back one if it ends up one past last index
    if (newIndex == length) {
      setCaretIndex(getCaretIndex() - 1);
    }
  }

  void normalModeBackwardChar(int count) {
    normalBackwardChar("", count);
  }

  void normalBackwardChar(String operator, int count) {
    int currentIndex = getCaretIndex();
    int newIndex = (currentIndex >= count) ? currentIndex - count : 0;

    if (operator.equals("")) {
      setCaretIndex(newIndex);
    } else if (operator.equals("d")) {
      editor.replacePartOfText("", newIndex, currentIndex);
    }
  }

  void normalModeTab() {
    if (editor.getSettings().isUseTabForAdvance() == true) {
      editor.nextEntry();
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
