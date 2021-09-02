package org.omegat.plugins.vimish;

import java.awt.event.*;
import javax.swing.Timer;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.omegat.util.Log;

class KeyChordsController {
  private List<String> keyChordUnderway = new ArrayList<String>();
  private Map<String, String> keyChordsHash;
  private Timer timer;
  private KeyEquivalenciesRouter keyEquivalenciesRouter;
  private Configuration configuration = Configuration.getConfiguration();

  KeyChordsController(KeyEquivalenciesRouter keyEquivalenciesRouter) {
    this.keyEquivalenciesRouter = keyEquivalenciesRouter;
    refreshKeyChordsHash();
  }

  void reset() {
    keyChordUnderway.clear();
  }

  void process(String keyString) {
    keyChordUnderway.add(keyString);
    if (keyChordUnderway.size() == 2) {
      String keyChordMatch = retrieveMatchingKeyChord(keyChordUnderway, keyChordsHash.keySet());
      // We also verify that characters in keyChordUnderway are unique
      // since it makes no sence to have a key chord with two
      // of the same characters
      if (keyChordMatch == null || keyChordUnderway.get(0) == keyChordUnderway.get(1)) {
        timer.stop();
        keyEquivalenciesRouter.sendMultipleKeysToKeyMapper(keyChordUnderway);
        reset();
      } else {
        timer.stop();
        String keyChordTranslation = keyChordsHash.get(keyChordMatch);
        String result = keyChordTranslation;
        keyEquivalenciesRouter.applyAsKeySequence(result);
        reset();
      }
    } else {
      if (isInOneOfTheKeyChords(keyChordUnderway, keyChordsHash.keySet())) {
        ActionListener taskPerformer = new ActionListener() {
          public void actionPerformed(ActionEvent _event) {
            Log.log("Key chord timed out");
            // This will run if timer times out before chord
            // completed
            String result = keyChordUnderway.get(0);
            // Result in this case will be a single key
            // since we're limited our total key chord size to 2
            keyEquivalenciesRouter.sendToKeyMapper(result);
            reset();
          }
        };

        int maxDelayInMilliseconds = 30;
        timer = new Timer(maxDelayInMilliseconds, taskPerformer);
        timer.setRepeats(false);
        timer.start();
      } else {
        String result = keyChordUnderway.get(0);
        keyEquivalenciesRouter.sendToKeyMapper(result);
        reset();
      }
    }
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

  // private static HashMap<String, String> getKeyChordsHash() {
  //   HashMap<String, String> keyChordsHash = new HashMap<String, String>();
  //   keyChordsHash.put("ie", "<ESC>");
  //   return keyChordsHash;
  // }

  void refreshKeyChordsHash() {
    KeyChords allKeyChords = getAllKeyChords();
    if (Mode.NORMAL.isActive()) {
      keyChordsHash = allKeyChords.normalModeKeyChords;
    } else if (Mode.VISUAL.isActive()){
      keyChordsHash = allKeyChords.visualModeKeyChords;
    } else {
      keyChordsHash = allKeyChords.insertModeKeyChords;
    }
  }

  KeyChords getAllKeyChords() {
    return configuration.getKeyChords();
  }
}
