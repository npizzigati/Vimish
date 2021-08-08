package org.omegat.plugins.vimish;

enum Mode {
  NORMAL (true),
  INSERT (false),
  VISUAL (false);

  private boolean active;
  Mode(boolean active) {
    this.active = active;
  }

  void activate() {
    // First deactivate all modes before activating correct one
    for (Mode mode : Mode.values()) {
      // Remove any highlighting created by visual mode
      switch (mode) {
      case VISUAL:
        if (mode.active) {
          Actions.clearVisualMarks();
        }
      default:
        mode.active = false;
      }
    }

    this.active = true;
    VimishCaret.processCaret();
  }

  boolean isActive() {
    return this.active;
  }
}
