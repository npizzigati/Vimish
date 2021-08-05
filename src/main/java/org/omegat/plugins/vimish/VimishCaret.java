package org.omegat.plugins.vimish;

import javax.swing.text.DefaultCaret;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Graphics;

import org.omegat.core.Core;
import org.omegat.gui.editor.EditorController;
import org.omegat.util.gui.Styles;
import org.omegat.util.Java8Compat;
import org.omegat.util.Log;

@SuppressWarnings("serial")

/**
 * This caret allows different caret shapes for Vimish modes.
 *
 * It will be used if Vimish can get a reference to the
 * the JTextComponent (EditorTextArea3) instance used as the main editing
 * area (through reflection).
 *
 * If it can't due to restrictions on reflection, the regular 
 * OmegaT caret will be used.
 *
 * Caret drawing logic has been taken from
 * OmegaT's OvertypeCaret class with minor modifications
 */
class VimishCaret extends DefaultCaret {
  private static VimishCaret instance;
  private static JTextComponent editingArea;

  private VimishCaret() {
  }

  static void setUpCaret() {
    editingArea = getEditingArea();
    if (editingArea != null) {
      VimishCaret caret = getVimishCaret();
      caret.setBlinkRate(editingArea.getCaret().getBlinkRate());
      editingArea.setCaret(caret);
      processCaret();
    } else {
      Log.log("Unable to set Vimish modal caret. Will use standard OmegaT caret");
    }
  }

  static VimishCaret getVimishCaret() {
    if (instance == null) {
      instance = new VimishCaret();
    }
    return instance;
  }

  private static JTextComponent getEditingArea() {
    EditorController editor = (EditorController) Core.getEditor();
    JTextComponent area = null;
    try {
        java.lang.reflect.Field protectedField = EditorController.class.getDeclaredField("editor");
        protectedField.setAccessible(true);
        area = (JTextComponent) protectedField.get(editor);
    } catch(NoSuchFieldException nsfe) {
        Log.log(nsfe);
    } catch(IllegalAccessException iae) {
        Log.log(iae);
    }
    return area;
  }

  static void processCaret() {
    // Invoking modelToView on the event dispatch thread, as
    // recommended by Java documentation for DefaultCaret

    // Do nothing if editingArea not accessible (e.g., if access
    // attempted to protected EditorTextArea3 member was denied)
    if (editingArea == null) return;

    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (Mode.NORMAL.isActive()) {
            // Change the caret shape, width and color
            editingArea.setCaretColor(Styles.EditorColor.COLOR_BACKGROUND.getColor());
            editingArea.putClientProperty("caretWidth", getCaretWidth());

            // We need to force the caret damage to have the rectangle to correctly show up,
            // otherwise half of the caret is shown.
            try {
              VimishCaret caret = getVimishCaret();

              Rectangle r = Java8Compat.modelToView(editingArea, caret.getDot());
              caret.damage(r);
            } catch (BadLocationException e) {
              e.printStackTrace();
            }
          } else {
            // reset to default insertMode caret
            editingArea.setCaretColor(Styles.EditorColor.COLOR_FOREGROUND.getColor());
            editingArea.putClientProperty("caretWidth", 1);
          }
        }
      });
  }

  /** Get the caret width from the size of the current letter. */
  public static int getCaretWidth() {
    FontMetrics fm = editingArea.getFontMetrics(editingArea.getFont());
    int carWidth = 1;
    try {
      carWidth = fm.stringWidth(editingArea.getText(editingArea.getCaretPosition(), 1));
    } catch (BadLocationException e) {
      /* empty */
    }
    return carWidth;
  }

  @Override
  public void paint(Graphics g) {
    if (Mode.NORMAL.isActive()) {
      int caretWidth = getCaretWidth();
      editingArea.putClientProperty("caretWidth", caretWidth);
      g.setXORMode(Styles.EditorColor.COLOR_FOREGROUND.getColor());
      g.translate(caretWidth / 2, 0);
      super.paint(g);
    } else {
      super.paint(g);
    }
  }

  @Override
  protected synchronized void damage(Rectangle r) {
    if (Mode.NORMAL.isActive()) {
      if (r != null) {
        int damageWidth = getCaretWidth();
        x = r.x - 4 - (damageWidth / 2);
        y = r.y;
        width = 9 + 3 * damageWidth / 2;
        height = r.height;
        repaint();
      }
    } else {
      super.damage(r);
    }
  }
}
