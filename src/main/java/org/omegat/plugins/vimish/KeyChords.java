package org.omegat.plugins.vimish;

import java.awt.event.*;
import javax.swing.Timer;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

class KeyChords {
  List<String> keyChordUnderway = new ArrayList<String>();
  HashMap<String, String> keyChordsHash = getKeyChordsHash();
  Timer timer;
  KeySequence keySequence;

  KeyChords(KeySequence keySequence) {
    this.keySequence = keySequence;
  }

  Boolean isActive() {
    return !keyChordsHash.isEmpty();
  }

  void process(String keyString) {
    keyChordUnderway.add(keyString);
    if (keyChordUnderway.size() == 2) {
      String keyChordMatch = retrieveMatchingKeyChord(keyChordUnderway, keyChordsHash.keySet());
      // We also verify that characters in keyChordUnderway are unique
      if (keyChordMatch == null || keyChordUnderway.get(0) == keyChordUnderway.get(1)) {
        timer.stop();
        keySequence.apply(String.join("", keyChordUnderway));
        keyChordUnderway.clear();
      } else {
        timer.stop();
        String keyChordTranslation = keyChordsHash.get(keyChordMatch);
        keySequence.apply(keyChordTranslation);
        keyChordUnderway.clear();
      }
    } else {
      if (isInOneOfTheKeyChords(keyChordUnderway, keyChordsHash.keySet())) {
        ActionListener taskPerformer = new ActionListener() {
          public void actionPerformed(ActionEvent _event) {
            keySequence.apply(String.join("", keyChordUnderway));
            keyChordUnderway.clear();
          }
        };

        int maxDelayInMilliseconds = 30;
        timer = new Timer(maxDelayInMilliseconds, taskPerformer);
        timer.setRepeats(false);
        timer.start();
      } else {
        keySequence.apply(keyChordUnderway.get(0));
        keyChordUnderway.clear();
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

  private static HashMap<String, String> getKeyChordsHash() {
    HashMap<String, String> keyChordsHash = new HashMap<String, String>();
    keyChordsHash.put("ie", "<ESC>");
    keyChordsHash.put("ah", "h");
    keyChordsHash.put("lt", "l");
    return keyChordsHash;
  }
}
