package org.omegat.plugins.vimish;

import org.omegat.gui.editor.EditorController;
import org.omegat.core.Core;
import org.omegat.gui.editor.IEditor.CaretPosition;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

class Actions {
  static EditorController editor = (EditorController) Core.getEditor(); 

  static void undo() {
    editor.undo();
  }

  static void clearVisualMarks() {
    VimishVisualMarker.resetMarks();
    editor.remarkOneMarker(VimishVisualMarker.class.getName());
  }

  static void visualDelete() {
    // Delete all visually selected text
    Integer startIndex = VimishVisualMarker.getMarkStart();
    Integer endIndex = VimishVisualMarker.getMarkEnd();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        editor.replacePartOfText("", startIndex, endIndex);
      }
    });
  }

  static void visualBackwardChar(int count) {
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

  static void visualForwardChar(int count) {
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

  static void normalForwardChar(String operator, int count) {
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

  static void normalBackwordChar(String operator, int count) {
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


  /**
   * Set caret position through OmegaT API
   */
  private static void setCaretIndex(int index) {
    CaretPosition newCaretPosition = new CaretPosition(index);
    editor.setCaretPosition(newCaretPosition);
  }

  /**
   * Get caret position from OmegaT API
   */
  private static int getCaretIndex() {
    return editor.getCurrentPositionInEntryTranslation();
  }
}
