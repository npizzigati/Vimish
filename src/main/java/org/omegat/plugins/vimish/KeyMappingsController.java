package org.omegat.plugins.vimish;

import java.awt.event.*;
import javax.swing.Timer;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.omegat.util.Log;

class KeyMappingsController {
  private List<String> keyMappingUnderway = new ArrayList<String>();
  private Map<String, String> currentKeyMappings;
  private Timer timer;
  private PreRouter preRouter;
  private Boolean isFirstKey = true;
  private Configuration configuration = Configuration.getConfiguration();

  KeyMappingsController(PreRouter preRouter) {
    this.preRouter = preRouter;
    refreshKeyMappings();
  }

  void processMultipleKeys(List<String> keyList) {
    // Send keys to be processed one at a time, since #process
    // can't handle input of multiple keys in a single string
    for (String key : keyList) {
      process(key);
    }
  }

  void process(String keyString) {
    if (currentKeyMappings == null || currentKeyMappings.isEmpty()) {
      preRouter.applyAsKeySequence(keyString);
      return;
    }
    // Pass keyString on to next stage (sequence evaluation) if
    // sequence evaluation already in progress, i.e., this is not
    // the first key in the key sequence being evaluated by
    // KeySequence.apply (e.g., if user presses an "f" (find) and then
    // an "a", even if the user has "aa" in their remap list, we
    // will not wait for the next "a").
    if (preRouter.isSequenceEvaluationInProgress()) {
      preRouter.applyAsKeySequence(keyString);
      return;
    }
    keyMappingUnderway.add(keyString);
    if (isFirstKey == true) {
      timerFirstStart();
      isFirstKey = false;
    } else {
      timer.restart();
    }

    List<String> matchCandidates = getMatchCandidates();
    int numberOfCandidates = matchCandidates.size();
    if (numberOfCandidates == 0) {
      // If there are no possible candidates in preceding keys
      // (because there has only been one key pressed)
      if (keyMappingUnderway.size() == 1) {
        preRouter.applyAsKeySequence(String.join("", keyMappingUnderway));
        reset();
      } else {
        // If there are possible candidates in preceding keys,
        // find the longest match and apply
        String longestMatch = getLongestMatch();
        if (longestMatch != null) {
          String matchValue = currentKeyMappings.get(longestMatch);
          preRouter.applyAsKeySequence(matchValue);
          List<String> remainder = getRemainder(longestMatch);
          reset();
          // Send the rest of the keys after the match for
          // reprocessing
          processMultipleKeys(remainder);
        } else {
          // If there is no match, send all keys in keyMappingUnderway
          preRouter.applyAsKeySequence(String.join("", keyMappingUnderway));
          reset();
        }
      }

    } else if (numberOfCandidates == 1) {
      // If only one candidate is found and it's a full match,
      // send on the map value
      String candidate = String.join("", keyMappingUnderway);
      String mapValue = currentKeyMappings.get(candidate);
      if (mapValue != null) {
        preRouter.applyAsKeySequence(mapValue);
        reset();
      }
    }
  }

  private List<String> getRemainder(String longestMatch) {
    String match = longestMatch;
    List<String> remainder = new ArrayList<String>();
    remainder.addAll(keyMappingUnderway);
    for (String key : keyMappingUnderway) {
      if (match.startsWith(key)) {
        match = match.replaceFirst(key, "");
        remainder.remove(0);
      } else {
        break;
      }
    }
    return remainder;
  }

  private List<String> getMatchCandidates() {
    List<String> candidates = new ArrayList<String>();
    String keyMappingUnderwayString = String.join("", keyMappingUnderway);
    for (String mapKey : currentKeyMappings.keySet()) {
      if (mapKey.startsWith(keyMappingUnderwayString)) {
        candidates.add(mapKey);
      }
    }
    return candidates;
  }

  KeyMappings getAllKeyMappings() {
    return configuration.getKeyMappings();
  }

  void refreshKeyMappings() {
    KeyMappings allKeyMappings = getAllKeyMappings();
    Map<String, String> userKeyMappings;
    if (Mode.NORMAL.isActive()) {
      userKeyMappings = allKeyMappings.normalModeKeyMappings;
    } else if (Mode.VISUAL.isActive()){
      userKeyMappings = allKeyMappings.visualModeKeyMappings;
    } else {
      userKeyMappings = allKeyMappings.insertModeKeyMappings;
    }
    currentKeyMappings = Util.normalizeTable(userKeyMappings);
  }

  private ActionListener createTaskPerformer() {
    ActionListener taskPerformer = new ActionListener() {
      public void actionPerformed(ActionEvent _event) {

        // Upon timeout, pick the longest candidate that also
        // matches keyMappingUnderway and send for evaluation
        String longestMatch = getLongestMatch();
        String matchValue = currentKeyMappings.get(longestMatch);
        if (matchValue != null) {
          preRouter.applyAsKeySequence(matchValue);
          List<String> remainder = getRemainder(longestMatch);
          reset();

          // Send the rest of the keys after the match for
          // reprocessing
          processMultipleKeys(remainder);

        } else {
          // If there is no match, send all keys in keyMappingUnderway
          preRouter.applyAsKeySequence(String.join("", keyMappingUnderway));
          reset();
        }
      }
    };

    return taskPerformer;
  }

  private String getLongestMatch() {
    String longestMatch = null;
    List<String> candidates = new ArrayList<String>(currentKeyMappings.keySet());
    candidates.sort((a, b) -> Integer.compare(b.length(), a.length()));

    String keyMappingUnderwayString = String.join("", keyMappingUnderway);
    for (String candidate : candidates) {
      if (keyMappingUnderwayString.startsWith(candidate)) {
        longestMatch = candidate;
      }
    }

    return longestMatch;
  }

  void timerFirstStart() {
    ActionListener taskPerformer = createTaskPerformer();

    int maxDelayInMilliseconds = 1000;
    timer = new Timer(maxDelayInMilliseconds, taskPerformer);
    timer.setRepeats(false);
    timer.start();
  }

  void reset() {
    if (timer.isRunning()) {
      timer.stop();
    }

    keyMappingUnderway.clear();
    isFirstKey = true;
  }
}
