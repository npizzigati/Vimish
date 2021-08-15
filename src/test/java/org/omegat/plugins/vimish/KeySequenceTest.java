package org.omegat.plugins.vimish;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.*;
import org.apache.commons.lang3.reflect.FieldUtils;

class KeySequenceTest {
  Actions mockActions;
  KeySequence keySequence;

  // @Test
  // void keySequence_h() {
  //   keySequence.apply("h");
  //   verify(mockActions).normalBackwardChar("", 1);
  //   assertThat(Mode.NORMAL.isActive()).as("In normal mode").isTrue();   
  // }

  // @Test
  // void keySequence_5h() {
  //   keySequence.apply("5h");
  //   verify(mockActions).normalBackwardChar("", 5);
  //   assertThat(Mode.NORMAL.isActive()).as("In normal mode now").isTrue();   
  //   assertThat(keySequenceResetToEmpty())
  //     .as("keySequence reset to empty").isTrue();
  // }

  // @Test
  // void keySequence_l() {
  //   keySequence.apply("l");
  //   verify(mockActions).normalForwardChar("", 1);
  //   assertThat(Mode.NORMAL.isActive()).as("In normal mode now").isTrue();   
  //   assertThat(keySequenceResetToEmpty())
  //     .as("keySequence reset to empty").isTrue();
  // }

  // @Test
  // void keySequence_5l() {
  //   keySequence.apply("5l");
  //   verify(mockActions).normalForwardChar("", 5);
  //   assertThat(Mode.NORMAL.isActive()).as("In normal mode now").isTrue();   
  //   assertThat(keySequenceResetToEmpty())
  //     .as("keySequence reset to empty").isTrue();
  // }

  // @Test
  // void keySequence_i() {
  //   keySequence.apply("i");
  //   assertThat(Mode.INSERT.isActive()).as("In insert mode now").isTrue();   
  //   assertThat(keySequenceResetToEmpty())
  //     .as("keySequence reset to empty").isTrue();
  // }

  // @Test
  // void keySequence_ESC() {
  //   Mode.INSERT.activate();
  //   keySequence.apply("ESC");
  //   assertThat(Mode.NORMAL.isActive()).as("In normal mode now").isTrue();   
  //   assertThat(keySequenceResetToEmpty())
  //     .as("keySequence reset to empty").isTrue();
  // }

  @BeforeEach
  void setUp() {
    Mode.NORMAL.activate();
    mockActions = mock(Actions.class);
    keySequence = new KeySequence(mockActions);
  }

  Boolean keySequenceResetToEmpty() {
    return getSequence().equals("");
  }

  String getSequence() {
    try {
      return (String) FieldUtils.readField(keySequence, "sequence", true);
    } catch(IllegalAccessException e) {
      return "No access";
    }
  }
}
