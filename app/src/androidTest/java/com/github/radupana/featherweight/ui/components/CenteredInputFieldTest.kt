package com.github.radupana.featherweight.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CenteredInputFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rpeField_acceptsDecimalInput() {
        var currentValue = TextFieldValue("")

        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.RPE,
                placeholder = "RPE",
            )
        }

        // Type "8.5"
        composeTestRule.onNodeWithText("RPE").performTextInput("8.5")
        assertEquals("8.5", currentValue.text)

        // Clear and type "9.25"
        composeTestRule.onNodeWithText("8.5").performTextReplacement("9.25")
        assertEquals("9.25", currentValue.text)

        // Clear and type "10.0"
        composeTestRule.onNodeWithText("9.25").performTextReplacement("10.0")
        assertEquals("10.0", currentValue.text)
    }

    @Test
    fun rpeField_rejectsInvalidDecimalFormats() {
        var currentValue = TextFieldValue("")

        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.RPE,
                placeholder = "RPE",
            )
        }

        // Try to type ".5" - should be rejected (can't start with decimal)
        composeTestRule.onNodeWithText("RPE").performTextInput(".5")
        assertEquals("", currentValue.text)

        // Type valid "8", then try to add ".." - second dot should be rejected
        composeTestRule.onNodeWithText("RPE").performTextInput("8")
        assertEquals("8", currentValue.text)

        composeTestRule.onNodeWithText("8").performTextReplacement("8.")
        assertEquals("8.", currentValue.text)

        composeTestRule.onNodeWithText("8.").performTextReplacement("8..")
        assertEquals("8.", currentValue.text) // Second dot rejected
    }

    @Test
    fun rpeField_clampsValuesAbove10() {
        var currentValue = TextFieldValue("")

        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.RPE,
                placeholder = "RPE",
            )
        }

        // Type "11" - should be clamped to "10"
        composeTestRule.onNodeWithText("RPE").performTextInput("11")
        assertEquals("10", currentValue.text)

        // Clear and type "10.5" - should be clamped to "10"
        composeTestRule.onNodeWithText("10").performTextReplacement("10.5")
        assertEquals("10", currentValue.text)

        // Clear and type "99" - should be clamped to "10"
        composeTestRule.onNodeWithText("10").performTextReplacement("99")
        assertEquals("10", currentValue.text)
    }

    @Test
    fun rpeField_respectsMaxLength() {
        var currentValue = TextFieldValue("")

        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.RPE,
                placeholder = "RPE",
            )
        }

        // Try to type "10.55" - should be rejected (too long, max 4 chars)
        composeTestRule.onNodeWithText("RPE").performTextInput("10.55")
        assertTrue(currentValue.text.length <= 4)
        assertTrue(currentValue.text != "10.55")
    }

    @Test
    fun placeholder_visibleWhenFieldEmpty() {
        var currentValue = TextFieldValue("")

        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.WEIGHT,
                placeholder = "70",
            )
        }

        // Placeholder should be visible when field is empty
        composeTestRule.onNodeWithText("70").assertIsDisplayed()
    }

    @Test
    fun placeholder_persistsOnFocus() {
        var currentValue = TextFieldValue("")

        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.WEIGHT,
                placeholder = "70",
            )
        }

        // Click on the field to focus it
        composeTestRule.onNodeWithText("70").performClick()

        // Placeholder should still be visible when focused but empty
        composeTestRule.onNodeWithText("70").assertIsDisplayed()
    }

    @Test
    fun placeholder_disappearsOnTyping() {
        var currentValue = TextFieldValue("")

        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.WEIGHT,
                placeholder = "70",
            )
        }

        // Placeholder should be visible initially
        composeTestRule.onNodeWithText("70").assertIsDisplayed()

        // Type a value
        composeTestRule.onNodeWithText("70").performTextInput("65")

        // The typed value should be present instead of placeholder
        composeTestRule.onNodeWithText("65").assertIsDisplayed()
    }

    @Test
    fun placeholder_updatesWithTargetValue() {
        var currentValue = TextFieldValue("")
        var placeholderText = "70"

        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.WEIGHT,
                placeholder = placeholderText,
            )
        }

        // Initial placeholder should be visible
        composeTestRule.onNodeWithText("70").assertIsDisplayed()

        // Update placeholder (simulating a target change)
        placeholderText = "80"
        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.WEIGHT,
                placeholder = placeholderText,
            )
        }

        // New placeholder should be visible
        composeTestRule.onNodeWithText("80").assertIsDisplayed()
    }

    @Test
    fun typedValue_overridesPlaceholder() {
        var currentValue = TextFieldValue("")

        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.WEIGHT,
                placeholder = "70",
            )
        }

        // Type a different value than the placeholder
        composeTestRule.onNodeWithText("70").performTextInput("65.5")

        // Verify the typed value is shown, not the placeholder
        assertEquals("65.5", currentValue.text)
        composeTestRule.onNodeWithText("65.5").assertIsDisplayed()
    }
}
