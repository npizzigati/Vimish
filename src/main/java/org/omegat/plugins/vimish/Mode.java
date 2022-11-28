package org.omegat.plugins.vimish;

enum Mode {
  NORMAL (true),
  INSERT (false),
  VISUAL (false),
  SEARCH (false),
  REPLACE (false);

  private boolean active;

  Mode(boolean active) {
    this.active = active;
  }

  void activate() {
    // First deactivate all modes before activating correct one
    for (Mode mode : Mode.values()) {
      mode.active = false;
    }

    this.active = true;
    VimishCaret.refreshCaret();

    // Flag for KeySequenceController to change the
    // mappings/chords/abbreviations to the correct mode
    Configuration.getConfiguration().flagKeyTablesRefreshNeeded();
  }

  boolean isActive() {
    return this.active;
  }
}
