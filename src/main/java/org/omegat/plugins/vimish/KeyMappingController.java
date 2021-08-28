package org.omegat.plugins.vimish;

import java.awt.event.*;
import javax.swing.Timer;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.omegat.util.Log;

class KeyMappingController {
  private List<String> keyMappingUnderway = new ArrayList<String>();
  private Map<String, String> keyMappingsHash;
  private Timer timer;
  private KeyEquivalenciesRouter keyEquivalenciesRouter;
  private Boolean isFirstKey = true;
  private Configuration configuration = Configuration.getConfiguration();

  KeyMappingController(KeyEquivalenciesRouter keyEquivalenciesRouter) {
    this.keyEquivalenciesRouter = keyEquivalenciesRouter;
    keyMappingsHash = getKeyMappingsHash();
  }


  void processMultipleKeys(List<String> keyList) {
    // Send keys to be processed one at a time, since #process
    // can't handle input of multiple keys in a single string
    for (String key : keyList) {
      process(key);
    }
  }

  void process(String keyString) {
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
        keyEquivalenciesRouter.applyAsKeySequence(String.join("", keyMappingUnderway));
        reset();
      } else {
        // If there are possible candidates in preceding keys,
        // find the longest match and apply
        String longestMatch = getLongestMatch();
        if (longestMatch != null) {
          String matchValue = keyMappingsHash.get(longestMatch);
          keyEquivalenciesRouter.applyAsKeySequence(matchValue);
          List<String> remainder = getRemainder(longestMatch);
          reset();
          // Send the rest of the keys after the match for
          // reprocessing
          processMultipleKeys(remainder);
        } else {
          // If there is no match, send all keys in keyMappingUnderway
          keyEquivalenciesRouter.applyAsKeySequence(String.join("", keyMappingUnderway));
          reset();
        }
      }

    } else if (numberOfCandidates == 1) {
      // If only one candidate is found and it's a full match,
      // send on the map value
      String candidate = String.join("", keyMappingUnderway);
      String mapValue = keyMappingsHash.get(candidate);
      if (mapValue != null) {
        keyEquivalenciesRouter.applyAsKeySequence(mapValue);
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
    for (String mapKey : keyMappingsHash.keySet()) {
      if (mapKey.startsWith(keyMappingUnderwayString)) {
        candidates.add(mapKey);
      }
    }
    return candidates;
  }

  Map<String, String> getKeyMappingsHash() {
    return configuration.getConfigKeyMappingsHash();
  }

  void refreshKeyMappingsHash() {
    keyMappingsHash = getKeyMappingsHash();
  }

  private ActionListener createTaskPerformer() {
    ActionListener taskPerformer = new ActionListener() {
      public void actionPerformed(ActionEvent _event) {

        // Upon timeout, pick the longest candidate that also
        // matches keyMappingUnderway and send for evaluation
        String longestMatch = getLongestMatch();
        String matchValue = keyMappingsHash.get(longestMatch);
        if (matchValue != null) {
          keyEquivalenciesRouter.applyAsKeySequence(matchValue);
          List<String> remainder = getRemainder(longestMatch);
          reset();

          // Send the rest of the keys after the match for
          // reprocessing
          processMultipleKeys(remainder);

        } else {
          // If there is no match, send all keys in keyMappingUnderway
          keyEquivalenciesRouter.applyAsKeySequence(String.join("", keyMappingUnderway));
          reset();
        }
      }
    };

    return taskPerformer;
  }

  private String getLongestMatch() {
    String longestMatch = null;
    List<String> candidates = new ArrayList<String>(keyMappingsHash.keySet());
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
