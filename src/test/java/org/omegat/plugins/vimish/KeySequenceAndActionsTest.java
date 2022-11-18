package org.omegat.plugins.vimish;

import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.IEditor.CaretPosition;
import org.omegat.gui.main.MainWindow;
import org.omegat.util.Log;

class ActionsTest {
  private EditorController mockEditor;
  private Actions actions;
  private MainWindow mockMainWindow;
  private KeySequence keySequence;

  // This is a stand-in for the real EditorController
  // instance. It receives text replacement and caret position
  // instructions (which would ordinarily be sent to the real
  // editor), so we can examine the text and caret position after
  // each key sequence is evaluated.
  class TestEditor {
    private String content;
    private int caretPos;
    TestEditor(final String initialContent, final int initialCaretPos) {
      content = initialContent;
      caretPos = initialCaretPos;
    }

    void replacePartOfText(final String newText, final int startIndex, final int endIndex) {
      if (endIndex > content.length()) {
        return;
      }
      content = content.substring(0, startIndex) + newText + content.substring(endIndex);
    }

    int insertText(final String text) {
      int index = getCaretPosition();
      // If we are in insert mode at end of content, caret
      // position will initially be 1 beyond end (content
      // length). We need to move it back before inserting to
      // avoid out of bounds error.
      if (index > content.length()) {
        index = content.length();
      }
      content = content.substring(0, index) + text + content.substring(index);
      int newIndex = index + text.length();
      return newIndex;
    }

    void setCaretPosition(final CaretPosition pos) {
      caretPos = extractCaretIndex(pos);
    }

    void setCaretPosition(final int idx) {
      caretPos = idx;
    }

    int getCaretPosition() {
      // If caretPos is null, it has not been explicitly set
      // through method under test or any of its callees
      return caretPos;
    }

    String getContent() {
      return content;
    }
  }

  @BeforeEach
  void setUp() {
    mockEditor = mock(EditorController.class);
    mockMainWindow = mock(MainWindow.class);
    actions = new Actions(mockEditor, mockMainWindow);
    keySequence = new KeySequence(actions);
    doNothing().when(mockMainWindow).showStatusMessageRB(isA(String.class), isA(String.class));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/normal_mode_delete_data.csv", numLinesToSkip = 1)
  void testNormalModeDelete(final String initialMode, final String initialCaretPos, final String initialContent, final String sequence,
                              final String expectedMode, final String expectedCaretPos, final String expectedContent) {
    performTest(initialMode, initialCaretPos, initialContent, sequence, expectedMode, expectedCaretPos, expectedContent);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/normal_mode_change_data.csv", numLinesToSkip = 1)
  void testNormalModeChange(final String initialMode, final String initialCaretPos, final String initialContent, final String sequence,
                              final String expectedMode, final String expectedCaretPos, final String expectedContent) {
    performTest(initialMode, initialCaretPos, initialContent, sequence, expectedMode, expectedCaretPos, expectedContent);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/normal_mode_move_data.csv", numLinesToSkip = 1)
  void testNormalModeMove(final String initialMode, final String initialCaretPos, final String initialContent, final String sequence,
                              final String expectedMode, final String expectedCaretPos, final String expectedContent) {
    performTest(initialMode, initialCaretPos, initialContent, sequence, expectedMode, expectedCaretPos, expectedContent);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/multiple_actions_data.csv", numLinesToSkip = 1)
  void testMultipleActions(final String initialMode, final String initialCaretPos, final String initialContent, final String sequence,
                              final String expectedMode, final String expectedCaretPos, final String expectedContent) {
    performTest(initialMode, initialCaretPos, initialContent, sequence, expectedMode, expectedCaretPos, expectedContent);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/multiple_actions_repeat_last_change_data.csv", numLinesToSkip = 1)
  void testMultipleActionsRepeatLastChange(final String initialMode, final String initialCaretPos, final String initialContent, final String sequence,
                              final String expectedMode, final String expectedCaretPos, final String expectedContent) {
    performTest(initialMode, initialCaretPos, initialContent, sequence, expectedMode, expectedCaretPos, expectedContent);
  }

  void performTest(final String initialMode, final String initialCaretPos, final String initialContent, final String sequence,
                              final String expectedMode, final String expectedCaretPos, final String expectedContent) {
    TestEditor testEditor = new TestEditor(initialContent, Integer.valueOf(initialCaretPos));
    setupStandInMethods(testEditor);
    Mode.valueOf(initialMode).activate();
    String normalizedSequence = Util.normalizeString(sequence);
    keySequence.apply(normalizedSequence);
    assertEquals(expectedContent, testEditor.getContent());
    assertTrue(Mode.valueOf(expectedMode).isActive());
    assertEquals(Integer.valueOf(expectedCaretPos), testEditor.getCaretPosition());
  }

  void setupStandInMethods(final TestEditor testEditor) {
    doAnswer((InvocationOnMock i) -> {
      return testEditor.getCaretPosition();
      }).when(mockEditor).getCurrentPositionInEntryTranslation();

    doAnswer((InvocationOnMock i) -> {
      return testEditor.getContent();
      }).when(mockEditor).getCurrentTranslation();

    doAnswer((InvocationOnMock i) -> {
      Object[] args = i.getArguments();
      String newText = (String) args[0];
      int startIndex = (Integer) args[1];
      int endIndex = (Integer) args[2];
      testEditor.replacePartOfText(newText, startIndex, endIndex);
      int newCaretIndex = startIndex + newText.length();
      testEditor.setCaretPosition(newCaretIndex);
      return null;
      }).when(mockEditor).replacePartOfText(anyString(), anyInt(), anyInt());

    doAnswer((InvocationOnMock i) -> {
      Object[] args = i.getArguments();
      String text = (String) args[0];
      int newIndex = testEditor.insertText(text);
      testEditor.setCaretPosition(newIndex);
      return null;
      }).when(mockEditor).insertText(anyString());

    doAnswer((InvocationOnMock i) -> {
      Object[] args = i.getArguments();
      testEditor.setCaretPosition((CaretPosition) args[0]);
      return null;
      }).when(mockEditor).setCaretPosition(any());
  }

  static Integer extractCaretIndex(final CaretPosition pos) {
    Integer index = null;
    try {
        java.lang.reflect.Field protectedField = CaretPosition.class.getDeclaredField("position");
        protectedField.setAccessible(true);
        index = (Integer) protectedField.get(pos);
    } catch (NoSuchFieldException nsfe) {
      Log.log("Unable to get caret index: " + nsfe);
    } catch (IllegalAccessException iae) {
      Log.log("Unable to get caret index: " + iae);
    }
    return index;
  }
}
