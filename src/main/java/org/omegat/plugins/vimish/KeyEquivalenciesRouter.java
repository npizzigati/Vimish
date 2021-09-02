package org.omegat.plugins.vimish;

import org.omegat.core.Core;
import org.omegat.gui.editor.EditorController;
import org.omegat.util.Log;

import java.util.List;

class KeyEquivalenciesRouter {
  private EditorController editor = (EditorController) Core.getEditor();
  private Actions actions = new Actions(editor);
  private KeySequence keySequence = new KeySequence(actions);
  private KeyMappingsController keyMappingsController = new KeyMappingsController(this);
  private KeyChordsController keyChordsController = new KeyChordsController(this);
  private Configuration configuration;

  KeyEquivalenciesRouter() {
    configuration = Configuration.getConfiguration();
  }

  void process(String keyString) {
    if (configuration.keyEquivalenciesNeedRefreshing()) {
      configuration.flagKeyEquivalenciesAsNotified();
      keyMappingsController.refreshKeyMappingsHash();
      keyChordsController.refreshKeyChordsHash();
      // TODO: refresh data in abbreviation and chord controllers
    }

    keyChordsController.process(keyString);
  }

  void sendToKeyMapper(String keyString) {
    keyMappingsController.process(keyString);
  }

  void applyAsKeySequence(String keyString) {
    keySequence.apply(keyString);
  }

  void sendMultipleKeysToKeyMapper(List<String> keyList) {
    keyMappingsController.processMultipleKeys(keyList);
  }
}
