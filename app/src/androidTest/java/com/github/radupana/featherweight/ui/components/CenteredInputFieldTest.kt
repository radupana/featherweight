package com.github.radupana.featherweight.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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
                placeholder = "RPE"
            )
        }
        
        // Type "8.5" 
        composeTestRule.onNodeWithText("RPE").performTextInput("8.5")
        assertThat(currentValue.text).isEqualTo("8.5")
        
        // Clear and type "9.25"
        composeTestRule.onNodeWithText("8.5").performTextReplacement("9.25")
        assertThat(currentValue.text).isEqualTo("9.25")
        
        // Clear and type "10.0"
        composeTestRule.onNodeWithText("9.25").performTextReplacement("10.0")
        assertThat(currentValue.text).isEqualTo("10.0")
    }
    
    @Test
    fun rpeField_rejectsInvalidDecimalFormats() {
        var currentValue = TextFieldValue("")
        
        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.RPE,
                placeholder = "RPE"
            )
        }
        
        // Try to type ".5" - should be rejected (can't start with decimal)
        composeTestRule.onNodeWithText("RPE").performTextInput(".5")
        assertThat(currentValue.text).isEqualTo("")
        
        // Type valid "8", then try to add ".." - second dot should be rejected
        composeTestRule.onNodeWithText("RPE").performTextInput("8")
        assertThat(currentValue.text).isEqualTo("8")
        
        composeTestRule.onNodeWithText("8").performTextReplacement("8.")
        assertThat(currentValue.text).isEqualTo("8.")
        
        composeTestRule.onNodeWithText("8.").performTextReplacement("8..")
        assertThat(currentValue.text).isEqualTo("8.") // Second dot rejected
    }
    
    @Test
    fun rpeField_clampsValuesAbove10() {
        var currentValue = TextFieldValue("")
        
        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.RPE,
                placeholder = "RPE"
            )
        }
        
        // Type "11" - should be clamped to "10"
        composeTestRule.onNodeWithText("RPE").performTextInput("11")
        assertThat(currentValue.text).isEqualTo("10")
        
        // Clear and type "10.5" - should be clamped to "10"
        composeTestRule.onNodeWithText("10").performTextReplacement("10.5")
        assertThat(currentValue.text).isEqualTo("10")
        
        // Clear and type "99" - should be clamped to "10"
        composeTestRule.onNodeWithText("10").performTextReplacement("99")
        assertThat(currentValue.text).isEqualTo("10")
    }
    
    @Test
    fun rpeField_respectsMaxLength() {
        var currentValue = TextFieldValue("")
        
        composeTestRule.setContent {
            CenteredInputField(
                value = currentValue,
                onValueChange = { currentValue = it },
                fieldType = InputFieldType.RPE,
                placeholder = "RPE"
            )
        }
        
        // Try to type "10.55" - should be rejected (too long, max 4 chars)
        composeTestRule.onNodeWithText("RPE").performTextInput("10.55")
        assertThat(currentValue.text.length).isAtMost(4)
        assertThat(currentValue.text).isNotEqualTo("10.55")
    }
}
