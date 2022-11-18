package org.omegat.plugins.vimish;

import java.awt.event.*;
import javax.swing.Timer;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

class KeyChordsController {
  private List<String> keyChordUnderway = new ArrayList<String>();
  private Map<String, String> currentKeyChords;
  private Timer timer;
  private PreRouter preRouter;
  private Configuration configuration = Configuration.getConfiguration();

  KeyChordsController(PreRouter preRouter) {
    this.preRouter = preRouter;
    refreshKeyChordsHash();
  }

  void reset() {
    keyChordUnderway.clear();
  }

  void process(String keyString) {
    if (currentKeyChords == null || currentKeyChords.isEmpty()) {
      preRouter.sendToKeyMapper(keyString);
      return;
    }
    keyChordUnderway.add(keyString);
    if (keyChordUnderway.size() == 2) {
      String keyChordMatch = retrieveMatchingKeyChord(keyChordUnderway, currentKeyChords.keySet());
      // We also verify that characters in keyChordUnderway are unique
      // since it makes no sense to have a key chord with two
      // of the same characters
      if (keyChordMatch == null || keyChordUnderway.get(0) == keyChordUnderway.get(1)) {
        timer.stop();
        preRouter.sendMultipleKeysToKeyMapper(keyChordUnderway);
        reset();
      } else {
        timer.stop();
        String keyChordTranslation = currentKeyChords.get(keyChordMatch);
        String result = keyChordTranslation;
        preRouter.applyAsKeySequence(result);
        reset();
      }
    } else {
      if (isInOneOfTheKeyChords(keyChordUnderway, currentKeyChords.keySet())) {
        ActionListener taskPerformer = new ActionListener() {
          public void actionPerformed(ActionEvent _event) {
            // This will run if timer times out before chord
            // completed
            String result = keyChordUnderway.get(0);
            // Result in this case will be a single key
            // since we've limited our total key chord size to 2
            preRouter.sendToKeyMapper(result);
            reset();
          }
        };

        int maxDelayInMilliseconds = 30;
        timer = new Timer(maxDelayInMilliseconds, taskPerformer);
        timer.setRepeats(false);
        timer.start();
      } else {
        String result = keyChordUnderway.get(0);
        preRouter.sendToKeyMapper(result);
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

  void refreshKeyChordsHash() {
    KeyChords allKeyChords = getAllKeyChords();
    Map<String, String> userKeyChordsHash;
    if (Mode.NORMAL.isActive()) {
      userKeyChordsHash = allKeyChords.normalModeKeyChords;
    } else if (Mode.VISUAL.isActive()){
      userKeyChordsHash = allKeyChords.visualModeKeyChords;
    } else {
      userKeyChordsHash = allKeyChords.insertModeKeyChords;
    }
    currentKeyChords = Util.normalizeTable(userKeyChordsHash);
  }

  KeyChords getAllKeyChords() {
    return configuration.getKeyChords();
  }
}
