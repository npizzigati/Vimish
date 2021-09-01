package org.omegat.plugins.vimish;

import org.omegat.core.Core;
import org.omegat.gui.editor.EditorController;
import org.omegat.util.Log;

import java.util.List;

class KeyEquivalenciesRouter {
  private EditorController editor = (EditorController) Core.getEditor();
  private Actions actions = new Actions(editor);
  private KeySequence keySequence = new KeySequence(actions);
  private KeyMappingController keyMappingController = new KeyMappingController(this);
  private KeyChordController keyChordController = new KeyChordController(this);
  private Configuration configuration;

  KeyEquivalenciesRouter() {
    configuration = Configuration.getConfiguration();
  }

  void process(String keyString) {
    if (configuration.keyEquivalenciesNeedRefreshing()) {
      configuration.flagKeyEquivalenciesAsNotified();
      keyMappingController.refreshKeyMappingsHash();
      // TODO: refresh data in abbreviation and chord controllers
    }

    keyChordController.process(keyString);
  }

  void sendToKeyMapper(String keyString) {
    keyMappingController.process(keyString);
  }

  void applyAsKeySequence(String keyString) {
    keySequence.apply(keyString);
  }

  void sendMultipleKeysToKeyMapper(List<String> keyList) {
    keyMappingController.processMultipleKeys(keyList);
  }
}
