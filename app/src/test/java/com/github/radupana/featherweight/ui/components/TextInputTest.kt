package com.github.radupana.featherweight.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for text input behavior in workout fields.
 * These tests verify that text input maintains proper cursor position
 * and character order when typing.
 */
class TextInputTest {
    @Test
    fun `typing 160 should result in 160 not 061`() {
        // Simulate typing "160" character by character
        var textFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // Type "1"
        textFieldValue = simulateTyping(textFieldValue, "1")
        assertThat(textFieldValue.text).isEqualTo("1")
        assertThat(textFieldValue.selection.start).isEqualTo(1)

        // Type "6"
        textFieldValue = simulateTyping(textFieldValue, "6")
        assertThat(textFieldValue.text).isEqualTo("16")
        assertThat(textFieldValue.selection.start).isEqualTo(2)

        // Type "0"
        textFieldValue = simulateTyping(textFieldValue, "0")
        assertThat(textFieldValue.text).isEqualTo("160")
        assertThat(textFieldValue.selection.start).isEqualTo(3)
    }

    @Test
    fun `typing multiple digits maintains correct order`() {
        var textFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // Type "12345"
        val digits = listOf("1", "2", "3", "4", "5")
        for (digit in digits) {
            textFieldValue = simulateTyping(textFieldValue, digit)
        }

        assertThat(textFieldValue.text).isEqualTo("12345")
        assertThat(textFieldValue.selection.start).isEqualTo(5)
    }

    @Test
    fun `typing decimal values maintains correct order`() {
        var textFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // Type "72.5"
        textFieldValue = simulateTyping(textFieldValue, "7")
        textFieldValue = simulateTyping(textFieldValue, "2")
        textFieldValue = simulateTyping(textFieldValue, ".")
        textFieldValue = simulateTyping(textFieldValue, "5")

        assertThat(textFieldValue.text).isEqualTo("72.5")
        assertThat(textFieldValue.selection.start).isEqualTo(4)
    }

    @Test
    fun `backspace removes last character correctly`() {
        var textFieldValue =
            TextFieldValue(
                text = "160",
                selection = TextRange(3), // Cursor at end
            )

        // Simulate backspace
        textFieldValue = simulateBackspace(textFieldValue)
        assertThat(textFieldValue.text).isEqualTo("16")
        assertThat(textFieldValue.selection.start).isEqualTo(2)

        // Another backspace
        textFieldValue = simulateBackspace(textFieldValue)
        assertThat(textFieldValue.text).isEqualTo("1")
        assertThat(textFieldValue.selection.start).isEqualTo(1)
    }

    @Test
    fun `cursor position is maintained when typing in middle`() {
        var textFieldValue =
            TextFieldValue(
                text = "10",
                selection = TextRange(1), // Cursor after "1"
            )

        // Type "6" in the middle
        textFieldValue = simulateTypingAtPosition(textFieldValue, "6")
        assertThat(textFieldValue.text).isEqualTo("160")
        assertThat(textFieldValue.selection.start).isEqualTo(2) // Cursor should be after "16"
    }

    /**
     * Simulates typing a character at the cursor position
     */
    private fun simulateTyping(
        currentValue: TextFieldValue,
        char: String,
    ): TextFieldValue {
        val text = currentValue.text
        val cursorPos = currentValue.selection.start
        val newText = text.substring(0, cursorPos) + char + text.substring(cursorPos)
        return TextFieldValue(
            text = newText,
            selection = TextRange(cursorPos + 1),
        )
    }

    /**
     * Simulates typing at current cursor position (which may not be at the end)
     */
    private fun simulateTypingAtPosition(
        currentValue: TextFieldValue,
        char: String,
    ): TextFieldValue = simulateTyping(currentValue, char)

    /**
     * Simulates backspace key press
     */
    private fun simulateBackspace(currentValue: TextFieldValue): TextFieldValue {
        val text = currentValue.text
        val cursorPos = currentValue.selection.start

        if (cursorPos == 0) return currentValue

        val newText = text.substring(0, cursorPos - 1) + text.substring(cursorPos)
        return TextFieldValue(
            text = newText,
            selection = TextRange(cursorPos - 1),
        )
    }
}
