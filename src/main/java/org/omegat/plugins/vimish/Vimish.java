/**
 * Vimish
 * Vim-style editing for OmegaT
 * 
 * @author Nick Pizzigati
 */

package org.omegat.plugins.vimish;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.*;
import javax.swing.Timer;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.EditorTextArea3;
import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.*;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.util.Log;

// TODO: options: - integrate system and vim clipboards
//                - position of cursor on entering insert mode
//                  (whether to adjust position by one)
//                - key or chord for escape
public class Vimish {
  static KeySequence keySequence;
  static boolean isFirstLoad = true;
  /**
   * Plugin loader
   */
  public static void loadPlugins() {
    Core.registerMarkerClass(VimishVisualMarker.class);
    CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
      @Override
      public void onApplicationStartup() {
        EditorController editor = (EditorController) Core.getEditor();
        Actions actions = new Actions(editor);
        keySequence = new KeySequence(actions);
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

  private static HashMap<String, String> getKeyChordsHash() {
    HashMap<String, String> keyChordsHash = new HashMap<String, String>();
    keyChordsHash.put("ie", "l");
    keyChordsHash.put("ah", "h");
    keyChordsHash.put("lt", "l");
    return keyChordsHash;
  }

  private static void installKeyEventDispatcher() {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyEventDispatcher() {
      List<String> keyChordUnderway = new ArrayList<String>();
      HashMap<String, String> keyChordsHash = getKeyChordsHash();
      Timer timer;

      @Override
      public boolean dispatchKeyEvent(KeyEvent event) {
        // Don't consume keys entered outside main editing area
        if (isOutsideMainEditingArea(event)) {
          return false;
        }

        // Don't consume action-key keyPressed events
        if (event.isActionKey() && event.getID() == KeyEvent.KEY_PRESSED) {
          return false;
        }

        // Consume other non-keyTyped events, except backspace in insert mode
        if (!(event.getID() == KeyEvent.KEY_TYPED)) {
          if (event.getID() == KeyEvent.KEY_PRESSED
              && event.getKeyCode() == KeyEvent.VK_BACK_SPACE
              && Mode.INSERT.isActive()) {
            return false;
          } else {
            return true;
          }
        }

        // In insert mode, pass keypress through to editing area
        // mode unless escape pressed
        // TODO: Change this so that it also sends Ctrl- sequences
        // to KeySequence#apply() for processing
        if (Mode.INSERT.isActive() &&
            (int)event.getKeyChar() != KeyEvent.VK_ESCAPE) {
          return false;
        }

        String keyString = determineKeyString(event);
        
        keyChordUnderway.add(keyString);
        if (keyChordUnderway.size() == 2) {
          Log.log("keyChordUnderway is now: " + keyChordUnderway);
          String keyChordMatch = retrieveMatchingKeyChord(keyChordUnderway, keyChordsHash.keySet());
          // We also verify that characters in keyChordUnderway are unique
          if (keyChordMatch == null || keyChordUnderway.get(0) == keyChordUnderway.get(1)) {
            Log.log("Key chord did not match. About to apply: " + keyChordUnderway);
            timer.stop();
            keySequence.apply(String.join("", keyChordUnderway));
            keyChordUnderway.clear();
          } else {
            Log.log("Key chord matched!");
            timer.stop();
            String keyChordTranslation = keyChordsHash.get(keyChordMatch);
            Log.log("Key chord equivalent: '" + keyChordTranslation + "'");
            keySequence.apply(keyChordTranslation);
            keyChordUnderway.clear();
          }
        } else {
          if (isInOneOfTheKeyChords(keyChordUnderway, keyChordsHash.keySet())) {
            Log.log("About to start timer");

            ActionListener taskPerformer = new ActionListener() {
              public void actionPerformed(ActionEvent _event) {
                Log.log("Running timer task (took too long)");
                Log.log("Going to apply: " + keyChordUnderway);
                keySequence.apply(String.join("", keyChordUnderway));
                keyChordUnderway.clear();
              }
            };
            int delayInMilliseconds = 50;
            timer = new Timer(delayInMilliseconds, taskPerformer);
            timer.setRepeats(false);
            timer.start();
          } else {
            keySequence.apply(keyChordUnderway.get(0));
            keyChordUnderway.clear();
          }
        }

        // This next line should be applied for each case as
        // appropriate in keyChord section, or uncommented if
        // keyChord section is not implemented.
        // keySequence.apply(keyString);

        // Consume keypress
        return true;

      }
    });
  }

  private static String retrieveMatchingKeyChord(List<String> keyChordUnderway,
                                      Set<String> keyChords) {
    for (String keyChord : keyChords) {
      if (isMatch(keyChordUnderway, keyChord)) return keyChord;
    }

    return null;
  }

  private static boolean isMatch(List<String> keyChordUnderway, String keyChord) {
    String keysEnteredString = String.join("", keyChordUnderway);
    String keysEnteredReverseString = keyChordUnderway.get(1) + keyChordUnderway.get(0);
    if (keysEnteredString.equals(keyChord) || keysEnteredReverseString.equals(keyChord)) {
      return true;
    } 
    return false;
  }

  private static boolean isInOneOfTheKeyChords(List<String> keyChordUnderway,
                                               Set<String> keyChords) {
    for (String keyChord : keyChords) {
      if (keyChord.contains(keyChordUnderway.get(0))) return true;
    }

    return false;
  }

  private static String determineKeyString(KeyEvent event) {
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

  private static boolean isOutsideMainEditingArea(KeyEvent event) {
    return !(event.getComponent().getClass() == EditorTextArea3.class);
  }
} 
