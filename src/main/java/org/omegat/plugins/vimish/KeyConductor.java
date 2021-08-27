package org.omegat.plugins.vimish;

import org.omegat.core.Core;
import org.omegat.gui.editor.EditorController;
import org.omegat.util.Log;

import java.util.List;

class KeyConductor {
  private EditorController editor = (EditorController) Core.getEditor();
  private Actions actions = new Actions(editor);
  private KeySequence keySequence = new KeySequence(actions);
  private KeyMappingProcessor keyMappingProcessor = new KeyMappingProcessor(this);
  private KeyChordProcessor keyChordProcessor = new KeyChordProcessor(this);
  private Configuration configuration;

  KeyConductor() {
    configuration = Configuration.getConfiguration();
  }

  void process(String keyString) {
    if (configuration.wereKeyEquivalenciesChanged()) {
      configuration.flagKeyEquivalenciesAsNotified();
      keyMappingProcessor.refreshKeyMappingsHash();
      // TODO: refresh data in abbreviation and chord controllers
    }

    keyChordProcessor.process(keyString);
  }

  void sendToKeyMapper(String keyString) {
    keyMappingProcessor.process(keyString);
  }

  void applyAsKeySequence(String keyString) {
    keySequence.apply(keyString);
  }

  void sendMultipleKeysToKeyMapper(List<String> keyList) {
    keyMappingProcessor.processMultipleKeys(keyList);
  }
}
