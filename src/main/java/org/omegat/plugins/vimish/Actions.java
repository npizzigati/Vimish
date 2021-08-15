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

  void visualYank() {
    Integer startIndex = VimishVisualMarker.getMarkStart();
    Integer endIndex = VimishVisualMarker.getMarkEnd();
    String currentTranslation = editor.getCurrentTranslation();
    String yankedText = currentTranslation.substring(startIndex, endIndex);
    Registers registers = Registers.getRegisters();

    registers.storeYankedText(yankedText);
    setCaretIndex(startIndex);
  }

  void visualDelete() {
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
  }

  void visualBackwardChar(int count) {
    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();
    String markOrientation = VimishVisualMarker.getMarkOrientation();
    int currentIndex;
    if (markOrientation == "leftOfCaret") {
      currentIndex = markStart;
    } else {
      currentIndex = markEnd - 1;
    }

    int newIndex = (currentIndex >= count) ? currentIndex - count : 0;
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

  void visualForwardChar(int count) {
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
    int newIndex = (length - currentIndex >= count) ? currentIndex + count : length;

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

  void normalPutSpecificRegister(String registerKey, String position) {
    Registers registers = Registers.getRegisters();
    String text = registers.retrieve(registerKey);
    normalPut(text, position);
  }

  void normalPutUnnamedRegister(String position) {
    Registers registers = Registers.getRegisters();
    String text = registers.retrieve("unnamed");
    normalPut(text, position);
  }

  void normalPut(String text, String position) {
    int index = getCaretIndex();
    if (position.equals("before")) {
      insertTextAtIndex(text, index);
    } else {
      insertTextAtIndex(text, index + 1);
    }
    normalBackwardChar(1);
  }

  void normalForwardChar(String operator, int count) {
    int currentIndex = getCaretIndex();
    int length = editor.getCurrentTranslation().length();
    int newIndex = (length - currentIndex >= count) ? currentIndex + count : length;

    Log.log("In normalForwardChar");
    if (operator.equals("")) {
      Log.log("About to set new caret index");
      setCaretIndex(newIndex);
      Log.log("Should have now set new caret index");
    } else if (operator.equals("d")) {
      editor.replacePartOfText("", currentIndex, newIndex);
    }
  }

  void normalBackwardChar(int count) {
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
