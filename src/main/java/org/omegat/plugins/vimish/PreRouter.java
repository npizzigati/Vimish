package org.omegat.plugins.vimish;

import org.omegat.core.Core;
import org.omegat.gui.editor.EditorController;
import org.omegat.gui.main.MainWindow;
import org.omegat.util.Log;

import java.util.List;

class PreRouter {
  private EditorController editor;
  private Actions actions;
  private KeySequence keySequence;
  private KeyMappingsController keyMappingsController;
  private KeyChordsController keyChordsController;
  private Configuration configuration;

  PreRouter() {
    editor = (EditorController) Core.getEditor();
    actions = new Actions(editor, (MainWindow) Core.getMainWindow());
    keySequence = new KeySequence(actions);
    keyMappingsController = new KeyMappingsController(this);
    keyChordsController = new KeyChordsController(this);
    configuration = Configuration.getConfiguration();
    actions.wiggleCaret();
  }

  boolean isSequenceEvaluationInProgress() {
    return keySequence.isInProgress();
  }

  void process(String keyString) {
    Log.log("Going to process keyString: " + keyString);
    if (configuration.keyTablesNeedRefreshing()) {
      configuration.flagKeyTablesRefreshNotified();
      keyMappingsController.refreshKeyMappings();
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
