package org.omegat.plugins.vimish;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.*;

import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.IEditor.CaretPosition;

class ActionsTest {
  EditorController mockEditor;
  Actions actions;

  @BeforeEach
  void setUp() {
    mockEditor = mock(EditorController.class);
    actions = new Actions(mockEditor);
  }

  @Test
  void testNormalModeForwardChar() {
    when(mockEditor.getCurrentPositionInEntryTranslation()).thenReturn(5);
    when(mockEditor.getCurrentTranslation()).thenReturn("This is a test");

    Actions actionsSpy = spy(actions); 
    actionsSpy.normalModeForwardChar("", 1);

    verify(actionsSpy).setCaretIndex(6);
  }

  @Test
  void testNormalModeForwardCharNearEndOfSegment() {
    // Must not set character index beyond end of segment
    when(mockEditor.getCurrentPositionInEntryTranslation()).thenReturn(10);
    when(mockEditor.getCurrentTranslation()).thenReturn("This is a test");

    Actions actionsSpy = spy(actions); 
    actionsSpy.normalModeForwardChar("", 6);

    verify(actionsSpy).setCaretIndex(14);
  }
}
