/**
 * Vimish
 * Vim-style editing for OmegaT
 * 
 * @author Nick Pizzigati
 */

package org.omegat.plugins.vimish;

import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.SwingUtilities;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.FontMetrics;
import java.awt.Rectangle;

// These are the imports I used in the groovy script
import org.omegat.gui.editor.IEditor;
import org.omegat.gui.editor.EditorTextArea3;
import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.EditorSettings;

import org.omegat.gui.main.MainWindow;
import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.*;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.util.Preferences;
import org.omegat.util.Java8Compat;
import org.omegat.util.gui.Styles;
import org.omegat.util.Log;

public class Vimish {
  static boolean isEditingSetupCompleted = false;
  static JTextComponent editingArea;
  static boolean normalMode = true;
  /**
   * Plugin loader
   */
  public static void loadPlugins() {
    CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
      @Override
      public void onApplicationStartup() {
        installEntryListener();
      }

      @Override
      public void onApplicationShutdown() {
      }
    });
  }

  private static void installEntryListener() {
    CoreEvents.registerEntryEventListener(new IEntryEventListener() {
      @Override
      public void onNewFile(String activeFileName) {
        if (isEditingSetupCompleted) return;

        setUpEditingArea();
        installKeyEventDispatcher();
      };

      @Override
      public void onEntryActivated(SourceTextEntry newEntry) {
      }
    });
  }

  private static void setUpEditingArea() {
    editingArea = getEditingArea();
    if (editingArea != null) {
      VimishCaret c = new VimishCaret();
      c.setBlinkRate(editingArea.getCaret().getBlinkRate());
      editingArea.setCaret(c);
      processCaret();
    } else {
      Log.log("Unable to set Vimish modal caret.");
    }
    isEditingSetupCompleted = true;
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

  private static void installKeyEventDispatcher() {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyEventDispatcher() {
      @Override
      public boolean dispatchKeyEvent(KeyEvent event) {

        // Don't consume action key keyPressed events
        if (event.isActionKey() && event.getID() == KeyEvent.KEY_PRESSED) {
          return false;
        }

        // Consume event if not a keyTyped event
        if (!(event.getID() == KeyEvent.KEY_TYPED)) {
          if (event.getID() == KeyEvent.KEY_PRESSED && event.getKeyCode() == 8
              && !normalMode) {
            return false;
          } else {
            return true;
          }
        }

        if (isOutsideMainEditingArea(event)) {
          JOptionPane.showMessageDialog(null, "key outside area");
          return true;
        }

        // try {
          if (!normalMode) {
            if ((int)event.getKeyChar() == 27) {
              normalMode = true;
              processCaret();
              return true;
            } else {
              return false;
            }
          } else {
            if (event.getKeyChar() == 'i') {
              normalMode = false;
              processCaret();
            } 
            return true;
          }
        // } finally {
        // }
      }
    });
  }

  protected static void processCaret() {
    // Invoking modelToView on the event dispatch thread, as
    // recommended by Java documentation for DefaultCaret

    // Do nothing if editingArea not accessible (e.g., if access
    // attempted to protected EditorTextArea3 member was denied)
    if (editingArea == null) return;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (normalMode) {
          // Change the caret shape, width and color
          editingArea.setCaretColor(Styles.EditorColor.COLOR_BACKGROUND.getColor());
          editingArea.putClientProperty("caretWidth", getCaretWidth());

          // We need to force the caret damage to have the rectangle to correctly show up,
          // otherwise half of the caret is shown.
          try {
              VimishCaret caret = (VimishCaret) editingArea.getCaret(); 
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
        JOptionPane.showMessageDialog(null, "caret width could not be determined");
          /* empty */
      }
      return carWidth;
  }

  // private static boolean isKeyTyped(KeyEvent event) {
  //   // Key location will be unknown for KeyTyped events
  //   return event.getKeyLocation() == KeyEvent.KEY_LOCATION_UNKNOWN;
  // }

  private static boolean isOutsideMainEditingArea(KeyEvent event) {
    return !(event.getComponent().getClass() == EditorTextArea3.class);
    // Class<?> clazz = event.getComponent().getClass();
    // return (JTextComponent.class.isAssignableFrom(clazz));
  }
} 




// class Listener implements KeyListener {
//   Listener() {
//     Vimish.editingArea.addKeyListener(this);
//   }

//   // void startListening() {
//   //   editingArea.addKeyListener(this);
//   // }

//   // void stopListening () {
//   //   editingArea.removeKeyListener(this);
//   // }

//   // OmegaT consumes the KeyPressed event when the
//   // "Advance on tab" option is selected, but check for it
//   // below (isIgnoredTab) in case that implementation is changed
//   // in the future
//   public void keyPressed(KeyEvent event) {
//     // Consume keyPressed event if keyTyped event also issued
//     // (key location will be unknown for KeyTyped events (except backspace and ??))
//     if (event.getKeyLocation() == KeyEvent.KEY_LOCATION_UNKNOWN ||
//       (event.getKeyCode()) == 8) {
//       event.consume();
//     }
//   }

//   public void keyTyped(KeyEvent event) {
//     String key = null;
//     if ((int)event.getKeyChar() == 8) {
//       key = "backspace";
//     } else if ((int)event.getKeyChar() == 27) {
//       key = "escape";
//     } else {
//       key = String.valueOf(event.getKeyChar());
//     }

//     if (Vimish.normalMode == false) {
//       if (key.equals("escape")) {
//         Vimish.normalMode = true;
//         Vimish.processCaret();
//         event.consume();
//       }
//     } else {
//       if (key.equals("i")) {
//         Vimish.normalMode = false;
//         Vimish.processCaret();
//       }
//       event.consume();
//     }
//   }

//   public void keyReleased(KeyEvent event) {
//     event.consume();
//   }
// }
