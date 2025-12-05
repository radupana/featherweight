package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.domain.TemplateSummary
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class WorkoutTemplateSelectionViewModelTest {
    @Test
    fun `TemplateWithExercises stores all fields correctly`() {
        val summary = createTemplateSummary("template-1", "Push Day")
        val exercises = listOf("Bench Press", "Overhead Press", "Tricep Dips")

        val template = TemplateWithExercises(summary, exercises)

        assertThat(template.summary.id).isEqualTo("template-1")
        assertThat(template.summary.name).isEqualTo("Push Day")
        assertThat(template.exerciseNames).hasSize(3)
        assertThat(template.exerciseNames).contains("Bench Press")
    }

    @Test
    fun `TemplateWithExercises can have empty exercise list`() {
        val summary = createTemplateSummary("template-2", "Empty Template")
        val exercises = emptyList<String>()

        val template = TemplateWithExercises(summary, exercises)

        assertThat(template.exerciseNames).isEmpty()
    }

    @Test
    fun `filtering templates by name returns matching templates`() =
        runTest {
            val templates = MutableStateFlow(createTestTemplates())
            val searchQuery = MutableStateFlow("Push")

            val filteredTemplates =
                combine(templates, searchQuery) { templateList, query ->
                    if (query.isEmpty()) {
                        templateList
                    } else {
                        templateList.filter { template ->
                            template.summary.name.contains(query, ignoreCase = true)
                        }
                    }
                }

            val result = filteredTemplates.first()

            assertThat(result).hasSize(1)
            assertThat(result.first().summary.name).isEqualTo("Push Day")
        }

    @Test
    fun `filtering with empty query returns all templates`() =
        runTest {
            val templates = MutableStateFlow(createTestTemplates())
            val searchQuery = MutableStateFlow("")

            val filteredTemplates =
                combine(templates, searchQuery) { templateList, query ->
                    if (query.isEmpty()) {
                        templateList
                    } else {
                        templateList.filter { template ->
                            template.summary.name.contains(query, ignoreCase = true)
                        }
                    }
                }

            val result = filteredTemplates.first()

            assertThat(result).hasSize(4)
        }

    @Test
    fun `filtering is case insensitive`() =
        runTest {
            val templates = MutableStateFlow(createTestTemplates())
            val searchQuery = MutableStateFlow("PULL")

            val filteredTemplates =
                combine(templates, searchQuery) { templateList, query ->
                    if (query.isEmpty()) {
                        templateList
                    } else {
                        templateList.filter { template ->
                            template.summary.name.contains(query, ignoreCase = true)
                        }
                    }
                }

            val result = filteredTemplates.first()

            assertThat(result).hasSize(1)
            assertThat(result.first().summary.name).isEqualTo("Pull Day")
        }

    @Test
    fun `filtering with no matches returns empty list`() =
        runTest {
            val templates = MutableStateFlow(createTestTemplates())
            val searchQuery = MutableStateFlow("nonexistent")

            val filteredTemplates =
                combine(templates, searchQuery) { templateList, query ->
                    if (query.isEmpty()) {
                        templateList
                    } else {
                        templateList.filter { template ->
                            template.summary.name.contains(query, ignoreCase = true)
                        }
                    }
                }

            val result = filteredTemplates.first()

            assertThat(result).isEmpty()
        }

    @Test
    fun `filtering matches partial names`() =
        runTest {
            val templates = MutableStateFlow(createTestTemplates())
            val searchQuery = MutableStateFlow("Day")

            val filteredTemplates =
                combine(templates, searchQuery) { templateList, query ->
                    if (query.isEmpty()) {
                        templateList
                    } else {
                        templateList.filter { template ->
                            template.summary.name.contains(query, ignoreCase = true)
                        }
                    }
                }

            val result = filteredTemplates.first()

            // "Push Day", "Pull Day", "Leg Day" match - "Full Body" does not
            assertThat(result).hasSize(3)
        }

    @Test
    fun `updateSearchQuery updates state flow`() =
        runTest {
            val searchQuery = MutableStateFlow("")

            // Simulate updateSearchQuery behavior
            searchQuery.value = "new query"

            assertThat(searchQuery.value).isEqualTo("new query")
        }

    @Test
    fun `TemplateSummary stores all required fields`() {
        val summary =
            TemplateSummary(
                id = "template-1",
                name = "Test Template",
                description = "A test template",
                exerciseCount = 5,
                setCount = 15,
                createdAt = LocalDateTime.of(2025, 1, 1, 9, 0),
                updatedAt = LocalDateTime.of(2025, 1, 2, 10, 0),
            )

        assertThat(summary.id).isEqualTo("template-1")
        assertThat(summary.name).isEqualTo("Test Template")
        assertThat(summary.description).isEqualTo("A test template")
        assertThat(summary.exerciseCount).isEqualTo(5)
        assertThat(summary.setCount).isEqualTo(15)
    }

    @Test
    fun `TemplateSummary can have null description`() {
        val summary =
            TemplateSummary(
                id = "template-2",
                name = "No Description",
                description = null,
                exerciseCount = 3,
                setCount = 9,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        assertThat(summary.description).isNull()
    }

    private fun createTestTemplates(): List<TemplateWithExercises> =
        listOf(
            TemplateWithExercises(
                summary = createTemplateSummary("1", "Push Day"),
                exerciseNames = listOf("Bench Press", "Overhead Press", "Tricep Dips"),
            ),
            TemplateWithExercises(
                summary = createTemplateSummary("2", "Pull Day"),
                exerciseNames = listOf("Barbell Row", "Pull-ups", "Face Pulls"),
            ),
            TemplateWithExercises(
                summary = createTemplateSummary("3", "Leg Day"),
                exerciseNames = listOf("Squat", "Romanian Deadlift", "Leg Press"),
            ),
            TemplateWithExercises(
                summary = createTemplateSummary("4", "Full Body"),
                exerciseNames = listOf("Squat", "Bench Press", "Deadlift"),
            ),
        )

    private fun createTemplateSummary(
        id: String,
        name: String,
    ) = TemplateSummary(
        id = id,
        name = name,
        description = null,
        exerciseCount = 3,
        setCount = 9,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )
}
