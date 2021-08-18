package org.omegat.plugins.vimish;

import java.awt.event.*;
import javax.swing.Timer;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.omegat.util.Log;

class KeyMappingProcessor {
  private List<String> keyMappingUnderway = new ArrayList<String>();
  private HashMap<String, String> keyMappingsHash;
  private Timer timer;
  private Dispatcher dispatcher;
  private Boolean isFirstKey = true;

  KeyMappingProcessor(Dispatcher dispatcher) {
    keyMappingsHash = getKeyMappingsHash();
    this.dispatcher = dispatcher;
  }

  private ActionListener createTaskPerformer() {
    ActionListener taskPerformer = new ActionListener() {
      public void actionPerformed(ActionEvent _event) {

        // Upon timeout, pick the longest candidate that also
        // matches keyMappingUnderway and send for evaluation
        String matchValue = getLongestMatch();
        if (matchValue != null) {
          dispatcher.applyAsKeySequence(matchValue);
        } else {
          // If there is no match, send all keys in keyMappingUnderway
          dispatcher.applyAsKeySequence(String.join("", keyMappingUnderway));
        } 
        reset();
      }
    };

    return taskPerformer;
  }

  private String getLongestMatch() {
    String matchValue = null;
    List<String> candidates = new ArrayList<String>(keyMappingsHash.keySet()); 
    candidates.sort((a, b) -> Integer.compare(b.length(), a.length()));
    Log.log("Sorted candidates: " + candidates);

    String keyMappingUnderwayString = String.join("", keyMappingUnderway); 
    for (String candidate : candidates) {
      if (keyMappingUnderwayString.startsWith(candidate)) {
        Log.log("Match found!");
        matchValue = keyMappingsHash.get(candidate);
      } 
    }

    return matchValue;
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
        dispatcher.applyAsKeySequence(String.join("", keyMappingUnderway));
        reset();
      } else {
        // If there are possible candidates in preceding keys,
        // find the longest match and apply
        String matchValue = getLongestMatch();
        if (matchValue != null) {
          dispatcher.applyAsKeySequence(matchValue);
        } else {
          // If there is no match, send all keys in keyMappingUnderway
          dispatcher.applyAsKeySequence(String.join("", keyMappingUnderway));
        } 
        reset();
      } 

    } else if (numberOfCandidates == 1) {
      // If only one candidate is found and it's a full match,
      // send on the map value
      String candidate = String.join("", keyMappingUnderway);
      String mapValue = keyMappingsHash.get(candidate); 
      if (mapValue != null) {
        dispatcher.applyAsKeySequence(mapValue);
        reset();
      }
    }
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

  private static HashMap<String, String> getKeyMappingsHash() {
    HashMap<String, String> keyMappingsHash = new HashMap<String, String>();
    keyMappingsHash.put("hw", "5h");
    keyMappingsHash.put("hwl", "5l");
    return keyMappingsHash;
  }
}
