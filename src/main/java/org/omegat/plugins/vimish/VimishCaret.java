/**
* Caret drawing logic has been taken from
* OmegaT's OvertypeCaret class with minor modifications
*/

package org.omegat.plugins.vimish;

import javax.swing.text.DefaultCaret;
import java.awt.Graphics;
import java.awt.Rectangle;
import org.omegat.util.gui.Styles;
// import org.omegat.gui.editor.EditorTextArea3;

@SuppressWarnings("serial")

class VimishCaret extends DefaultCaret {
  // private static VimishCaret caret;
  // private static EditorTextArea3 editingArea;

  // VimishCaret() {
  //   caret = new VimishCaret();
  // }

  // static void setUpCaret() {
  //   editingArea = getEditingArea();
  // }

  @Override
  public void paint(Graphics g) {
    if (Vimish.normalMode) {
      int caretWidth = Vimish.getCaretWidth();
      Vimish.editingArea.putClientProperty("caretWidth", caretWidth);
      g.setXORMode(Styles.EditorColor.COLOR_FOREGROUND.getColor());
      g.translate(caretWidth / 2, 0);
      super.paint(g);
    } else {
      super.paint(g);
    }
  }

  @Override
  protected synchronized void damage(Rectangle r) {
    if (Vimish.normalMode) {
      if (r != null) {
        int damageWidth = Vimish.getCaretWidth();
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
