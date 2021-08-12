package org.omegat.plugins.vimish;

import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.IEditor.CaretPosition;
// import org.omegat.util.Log;

import javax.swing.SwingUtilities;

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
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        editor.replacePartOfText("", startIndex, endIndex);
      }
    });
  }

  void visualBackwardChar(int count) {
    int currentIndex = getCaretIndex();
    int newIndex = (currentIndex >= count) ? currentIndex - count : 0;

    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();
    // If no mark yet
    if (markStart == null) {
      VimishVisualMarker.setMarkStart(newIndex);
      VimishVisualMarker.setMarkEnd(newIndex + 1);
    } else if (markStart <= newIndex) {
      VimishVisualMarker.setMarkEnd(newIndex + 1);
    } else {
      // If passing back over original start, set new markEnd to
      // current markStart (subtract 1 from markEnd since visual
      // mark is implemented up to but not including end index)
      if (currentIndex == markEnd - 1) {
        VimishVisualMarker.setMarkEnd(markStart + 1);
      }

      VimishVisualMarker.setMarkStart(newIndex);
    }

    editor.remarkOneMarker(VimishVisualMarker.class.getName());
    setCaretIndex(newIndex);
  }

  void visualForwardChar(int count) {
    int currentIndex = getCaretIndex();
    int length = editor.getCurrentTranslation().length();
    int newIndex = (length - currentIndex >= count) ? currentIndex + count : length;

    Integer markStart = VimishVisualMarker.getMarkStart();
    Integer markEnd = VimishVisualMarker.getMarkEnd();
    // If no mark yet
    if (markEnd == null) {
      VimishVisualMarker.setMarkStart(currentIndex);
      VimishVisualMarker.setMarkEnd(newIndex + 1);
    } else if (newIndex < markEnd) {
      VimishVisualMarker.setMarkStart(newIndex);
    } else {
      // If passing back over original start, set new markStart to
      // current markEnd (subtract 1 from markEnd since visual
      // mark is implemented up to but not including end index)
      if (currentIndex == markStart && markEnd - markStart > 1) {
        VimishVisualMarker.setMarkStart(markEnd - 1);
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
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (position.equals("before")) {
          insertTextAtIndex(text, index);
        } else {
          insertTextAtIndex(text, index + 1);
        }
        normalBackwardChar(1);
      }
    });
  }

  void normalForwardChar(String operator, int count) {
    int currentIndex = getCaretIndex();
    int length = editor.getCurrentTranslation().length();
    int newIndex = (length - currentIndex >= count) ? currentIndex + count : length;

    if (operator.equals("")) {
      setCaretIndex(newIndex);
    } else if (operator.equals("d")) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          editor.replacePartOfText("", currentIndex, newIndex);
        }
      });
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
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          editor.replacePartOfText("", newIndex, currentIndex);
        }
      });
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
