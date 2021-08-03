/**
 * This logic has been taken directly from 
 * OmegaT's OvertypeCaret class
 * with very minor modifications
 */

package org.omegat.plugins.vimish;

import javax.swing.text.DefaultCaret;
import java.awt.Graphics;
import java.awt.Rectangle;
import org.omegat.util.gui.Styles;

@SuppressWarnings("serial")
class VimishCaret extends DefaultCaret {
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
