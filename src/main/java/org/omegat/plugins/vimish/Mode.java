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
    for (Mode mode : Mode.values()) {
      mode.active = false;
    }
    this.active = true;
  }

  boolean isActive() {
    return this.active;
  }
}
