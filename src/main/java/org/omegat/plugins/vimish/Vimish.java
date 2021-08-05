/**
 * Vimish
 * Vim-style editing for OmegaT
 * 
 * @author Nick Pizzigati
 */

package org.omegat.plugins.vimish;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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
import org.omegat.util.gui.Styles;
import org.omegat.util.Log;

// TODO: options: integrate system and vim clipboards
//                where the cursor is when entering insert mode
//                (whether to adjust position by one)
public class Vimish {
  static boolean isFirstLoad = true;
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
        if (isFirstLoad) {
          VimishCaret.setUpCaret();
          installKeyEventDispatcher();
          isFirstLoad = false;
        }
      };

      @Override
      public void onEntryActivated(SourceTextEntry newEntry) {
      }
    });
  }

  private static void installKeyEventDispatcher() {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyEventDispatcher() {
      @Override
      public boolean dispatchKeyEvent(KeyEvent event) {

        // Don't consume action-key keyPressed events
        if (event.isActionKey() && event.getID() == KeyEvent.KEY_PRESSED) {
          return false;
        }

        // Consume other non-keyTyped events, except backspace in insert mode
        if (!(event.getID() == KeyEvent.KEY_TYPED)) {
          if (event.getID() == KeyEvent.KEY_PRESSED
              && event.getKeyCode() == KeyEvent.VK_BACK_SPACE
              && !normalMode) {
            return false;
          } else {
            return true;
          }
        }

        // Consume keys entered outside main editing area
        if (isOutsideMainEditingArea(event)) {
          return true;
        }

        KeySequence sequence = KeySequence.getKeySequence();
        String keyString = determineKeyString(event);

        sequence.applyKey(keyString);

        if (!normalMode) {
          if ((int)event.getKeyChar() == 27) {
            normalMode = true;
            sequence.resetSequence();
            VimishCaret.processCaret();
            return true;
          } else {
            return false;
          }
        } else {
          // Normal mode
          if (event.getKeyChar() == 'i') {
            normalMode = false;
            VimishCaret.processCaret();
          } 
          return true;
        }
      }
    });
  }

  static String determineKeyString(KeyEvent event) {
    String keyString = null;
    char keyChar = event.getKeyChar();
    // Do I also have to determine the key code for enter?
    if ((int)keyChar == KeyEvent.VK_ESCAPE) {
      keyString = "ESC";
    } else {
      keyString = String.valueOf(keyChar);
    }
    return keyString;
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
