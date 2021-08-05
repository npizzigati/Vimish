package org.omegat.plugins.vimish;

import org.omegat.gui.editor.EditorController;
import org.omegat.core.Core;
import org.omegat.gui.editor.IEditor.CaretPosition;

import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

class Actions {
  static EditorController editor = (EditorController) Core.getEditor(); 

  /**
   * Move caret index position backward count times, applying
   * operator if present
   */
  static void backwordChar(String operator, int count) {
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
   * Move caret index position forward count times, applying
   * operator if present
   */
  static void forwardChar(String operator, int count) {
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

  /**
   * Set caret position by calling OmegaT API
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
