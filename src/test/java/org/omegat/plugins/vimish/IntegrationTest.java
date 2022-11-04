package org.omegat.plugins.vimish;

import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.IEditor.CaretPosition;
import org.omegat.util.Log;

import java.lang.Integer;

class ActionsTest {
  EditorController mockEditor;
  Actions actions;
  KeySequence keySequence;

  // This is a stand-in for the real EditorController instance
  // (in addition to the mock created below). The mock editor
  // provides mock return values for function calls that would
  // otherwise involve other parts of OmegaT which we don't have
  // access to here. This stand-in, on the other hand, receives
  // text replacement and caret position instructions (which would
  // ordinarily be sent to the real editor), so we can examine
  // the text and caret position after each key sequence is
  // evaluated.
  class TestEditor {
    private String content;
    private int caretPos;
    TestEditor(String initialContent, int initialCaretPos) {
      content = initialContent;
      caretPos = initialCaretPos;
    }

    void replacePartOfText(String newText, int startIndex, int endIndex) {
      if (endIndex > content.length()) {
        return;
      }
      content = content.substring(0, startIndex) + newText + content.substring(endIndex);
    }

    int insertText(String text) {
      int index = getCaretPosition();
      content = content.substring(0, index) + text + content.substring(index);
      int newIndex = index + text.length();
      return newIndex;
    }

    void setCaretPosition(CaretPosition pos) {
      caretPos = extractCaretIndex(pos);
      getCaretPosition();
    }

    void setCaretPosition(int idx) {
      caretPos = idx;
      getCaretPosition();
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
    Log.log(System.currentTimeMillis() + " Starting test case: ");
    mockEditor = mock(EditorController.class);
    actions = new Actions(mockEditor);
    keySequence = new KeySequence(actions);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/normal_mode_delete_data.csv", numLinesToSkip = 1)
  void testNormalModeDelete(String initialMode, String initialCaretPos, String initialContent, String sequence,
                              String expectedMode, String expectedCaretPos, String expectedContent) {
    performTest(initialMode, initialCaretPos, initialContent, sequence, expectedMode, expectedCaretPos, expectedContent);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/normal_mode_change_data.csv", numLinesToSkip = 1)
  void testNormalModeChange(String initialMode, String initialCaretPos, String initialContent, String sequence,
                              String expectedMode, String expectedCaretPos, String expectedContent) {
    performTest(initialMode, initialCaretPos, initialContent, sequence, expectedMode, expectedCaretPos, expectedContent);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/normal_mode_move_data.csv", numLinesToSkip = 1)
  void testNormalModeMove(String initialMode, String initialCaretPos, String initialContent, String sequence,
                              String expectedMode, String expectedCaretPos, String expectedContent) {
    performTest(initialMode, initialCaretPos, initialContent, sequence, expectedMode, expectedCaretPos, expectedContent);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/multiple_actions_data.csv", numLinesToSkip = 1)
  void testMultipleActions(String initialMode, String initialCaretPos, String initialContent, String sequence,
                              String expectedMode, String expectedCaretPos, String expectedContent) {
    performTest(initialMode, initialCaretPos, initialContent, sequence, expectedMode, expectedCaretPos, expectedContent);
  }

  void performTest(String initialMode, String initialCaretPos, String initialContent, String sequence,
                              String expectedMode, String expectedCaretPos, String expectedContent) {
    TestEditor testEditor = new TestEditor(initialContent, Integer.valueOf(initialCaretPos));
    setupStandInMethods(testEditor);
    Mode.valueOf(initialMode).activate();
    keySequence.apply(sequence);
    assertEquals(expectedContent, testEditor.getContent());
    assertTrue(Mode.valueOf(expectedMode).isActive());
    assertEquals(Integer.valueOf(expectedCaretPos), testEditor.getCaretPosition());
  }

  void setupStandInMethods(TestEditor testEditor) {
    doAnswer((InvocationOnMock i) -> {
      return testEditor.getCaretPosition();
      }).when(mockEditor).getCurrentPositionInEntryTranslation();

    doAnswer((InvocationOnMock i) -> {
      return testEditor.getContent();
      }).when(mockEditor).getCurrentTranslation();

    doAnswer((InvocationOnMock i) -> {
      Object[] args = i.getArguments();
      String newText = (String)args[0];
      int startIndex = (Integer)args[1];
      int endIndex = (Integer)args[2];
      testEditor.replacePartOfText(newText, startIndex, endIndex);
      int newCaretIndex = startIndex + newText.length();
      testEditor.setCaretPosition(newCaretIndex);
      return null;
      }).when(mockEditor).replacePartOfText(anyString(), anyInt(), anyInt());

    doAnswer((InvocationOnMock i) -> {
      Object[] args = i.getArguments();
      String text = (String)args[0];
      int newIndex = testEditor.insertText(text);
      testEditor.setCaretPosition(newIndex);
      return null;
      }).when(mockEditor).insertText(anyString());

    doAnswer((InvocationOnMock i) -> {
      Object[] args = i.getArguments();
      testEditor.setCaretPosition((CaretPosition)args[0]);
      return null;
      }).when(mockEditor).setCaretPosition(any());
  }

  static Integer extractCaretIndex(CaretPosition pos) {
    Integer index = null;
    try {
        java.lang.reflect.Field protectedField = CaretPosition.class.getDeclaredField("position");
        protectedField.setAccessible(true);
        index = (Integer) protectedField.get(pos);
    } catch(NoSuchFieldException nsfe) {
      Log.log("Unable to get caret index: " + nsfe);
    } catch(IllegalAccessException iae) {
      Log.log("Unable to get caret index: " + iae);
    }
    return index;
  }
}
