package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Test suite for TrainingAnalysisDao.
 *
 * Tests all DAO methods including:
 * - CRUD operations
 * - Date-based queries
 * - User filtering
 * - JSON field handling
 * - Latest analysis retrieval
 */
@RunWith(RobolectricTestRunner::class)
class TrainingAnalysisDaoTest : BaseDaoTest() {
    // Helper Methods

    /**
     * Creates a test TrainingAnalysis.
     */
    private fun createAnalysis(
        id: String = "test-analysis",
        userId: String? = "test-user",
        analysisDate: LocalDateTime = LocalDateTime.now(),
        periodStart: LocalDate = LocalDate.now().minusDays(7),
        periodEnd: LocalDate = LocalDate.now(),
        overallAssessment: String = "Good progress",
        keyInsightsJson: String = "[]",
        recommendationsJson: String = "[]",
        warningsJson: String = "[]",
        adherenceAnalysisJson: String? = null,
    ): TrainingAnalysis =
        TrainingAnalysis(
            id = id,
            userId = userId,
            analysisDate = analysisDate,
            periodStart = periodStart,
            periodEnd = periodEnd,
            overallAssessment = overallAssessment,
            keyInsightsJson = keyInsightsJson,
            recommendationsJson = recommendationsJson,
            warningsJson = warningsJson,
            adherenceAnalysisJson = adherenceAnalysisJson,
        )

    // CRUD Operations Tests

    @Test
    fun `insertAnalysis should add analysis to database`() =
        runTest {
            val analysis = createAnalysis()

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(analysis.id)
            assertThat(retrieved?.userId).isEqualTo("test-user")
            assertThat(retrieved?.overallAssessment).isEqualTo("Good progress")
        }

    @Test
    fun `insertAnalysis with REPLACE should update existing analysis`() =
        runTest {
            val analysis = createAnalysis(overallAssessment = "Original assessment")

            trainingAnalysisDao.insertAnalysis(analysis)

            val updated = analysis.copy(overallAssessment = "Updated assessment")
            trainingAnalysisDao.insertAnalysis(updated)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.overallAssessment).isEqualTo("Updated assessment")
        }

    @Test
    fun `getAnalysisById should return specific analysis`() =
        runTest {
            val analysis = createAnalysis()

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)

            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(analysis.id)
            assertThat(retrieved?.overallAssessment).isEqualTo("Good progress")
        }

    @Test
    fun `getAnalysisById should return null for non-existent id`() =
        runTest {
            val retrieved = trainingAnalysisDao.getAnalysisById("non-existent-id")

            assertThat(retrieved).isNull()
        }

    @Test
    fun `getAllAnalyses should return all analyses in database`() =
        runTest {
            val analysis1 = createAnalysis(id = "analysis-1", userId = "user-1")
            val analysis2 = createAnalysis(id = "analysis-2", userId = "user-2")
            val analysis3 = createAnalysis(id = "analysis-3", userId = "user-1")

            trainingAnalysisDao.insertAnalysis(analysis1)
            trainingAnalysisDao.insertAnalysis(analysis2)
            trainingAnalysisDao.insertAnalysis(analysis3)

            val allAnalyses = trainingAnalysisDao.getAllAnalyses()

            assertThat(allAnalyses).hasSize(3)
            assertThat(allAnalyses.map { it.id }).containsExactly("analysis-1", "analysis-2", "analysis-3")
        }

    // Query Operations Tests

    @Test
    fun `getLatestAnalysis should return most recent analysis by date`() =
        runTest {
            val oldAnalysis =
                createAnalysis(
                    id = "old-analysis",
                    analysisDate = LocalDateTime.now().minusDays(7),
                    overallAssessment = "Old analysis",
                )
            val middleAnalysis =
                createAnalysis(
                    id = "middle-analysis",
                    analysisDate = LocalDateTime.now().minusDays(3),
                    overallAssessment = "Middle analysis",
                )
            val recentAnalysis =
                createAnalysis(
                    id = "recent-analysis",
                    analysisDate = LocalDateTime.now(),
                    overallAssessment = "Recent analysis",
                )

            trainingAnalysisDao.insertAnalysis(oldAnalysis)
            trainingAnalysisDao.insertAnalysis(middleAnalysis)
            trainingAnalysisDao.insertAnalysis(recentAnalysis)

            val latest = trainingAnalysisDao.getLatestAnalysis()

            assertThat(latest).isNotNull()
            assertThat(latest?.id).isEqualTo("recent-analysis")
            assertThat(latest?.overallAssessment).isEqualTo("Recent analysis")
        }

    @Test
    fun `getLatestAnalysis should return null when no analyses exist`() =
        runTest {
            val latest = trainingAnalysisDao.getLatestAnalysis()

            assertThat(latest).isNull()
        }

    @Test
    fun `getLatestAnalysis should return only one analysis`() =
        runTest {
            val analysis1 = createAnalysis(id = "analysis-1", analysisDate = LocalDateTime.now().minusDays(2))
            val analysis2 = createAnalysis(id = "analysis-2", analysisDate = LocalDateTime.now().minusDays(1))
            val analysis3 = createAnalysis(id = "analysis-3", analysisDate = LocalDateTime.now())

            trainingAnalysisDao.insertAnalysis(analysis1)
            trainingAnalysisDao.insertAnalysis(analysis2)
            trainingAnalysisDao.insertAnalysis(analysis3)

            val latest = trainingAnalysisDao.getLatestAnalysis()

            assertThat(latest).isNotNull()
            assertThat(latest?.id).isEqualTo("analysis-3")
        }

    // Deletion Operations Tests

    @Test
    fun `deleteAllForUser should remove all analyses for specific user`() =
        runTest {
            val user1Analysis1 = createAnalysis(id = "user1-analysis1", userId = "user-1")
            val user1Analysis2 = createAnalysis(id = "user1-analysis2", userId = "user-1")
            val user2Analysis = createAnalysis(id = "user2-analysis", userId = "user-2")

            trainingAnalysisDao.insertAnalysis(user1Analysis1)
            trainingAnalysisDao.insertAnalysis(user1Analysis2)
            trainingAnalysisDao.insertAnalysis(user2Analysis)

            trainingAnalysisDao.deleteAllForUser("user-1")

            val allAnalyses = trainingAnalysisDao.getAllAnalyses()
            assertThat(allAnalyses).hasSize(1)
            assertThat(allAnalyses[0].userId).isEqualTo("user-2")
        }

    @Test
    fun `deleteAllForUser should not affect other users`() =
        runTest {
            val user1Analysis = createAnalysis(id = "user1-analysis", userId = "user-1")
            val user2Analysis = createAnalysis(id = "user2-analysis", userId = "user-2")

            trainingAnalysisDao.insertAnalysis(user1Analysis)
            trainingAnalysisDao.insertAnalysis(user2Analysis)

            trainingAnalysisDao.deleteAllForUser("user-1")

            val retrieved = trainingAnalysisDao.getAnalysisById("user2-analysis")
            assertThat(retrieved).isNotNull()
        }

    @Test
    fun `deleteAllWhereUserIdIsNull should remove analyses with null userId`() =
        runTest {
            val analysisWithUser = createAnalysis(id = "with-user", userId = "user-1")
            val analysisWithoutUser1 = createAnalysis(id = "without-user-1", userId = null)
            val analysisWithoutUser2 = createAnalysis(id = "without-user-2", userId = null)

            trainingAnalysisDao.insertAnalysis(analysisWithUser)
            trainingAnalysisDao.insertAnalysis(analysisWithoutUser1)
            trainingAnalysisDao.insertAnalysis(analysisWithoutUser2)

            trainingAnalysisDao.deleteAllWhereUserIdIsNull()

            val allAnalyses = trainingAnalysisDao.getAllAnalyses()
            assertThat(allAnalyses).hasSize(1)
            assertThat(allAnalyses[0].id).isEqualTo("with-user")
        }

    @Test
    fun `deleteAllWhereUserIdIsNull should not affect analyses with userId`() =
        runTest {
            val analysisWithUser = createAnalysis(id = "with-user-2", userId = "user-1")
            val analysisWithoutUser = createAnalysis(id = "without-user-3", userId = null)

            trainingAnalysisDao.insertAnalysis(analysisWithUser)
            trainingAnalysisDao.insertAnalysis(analysisWithoutUser)

            trainingAnalysisDao.deleteAllWhereUserIdIsNull()

            val allAnalyses = trainingAnalysisDao.getAllAnalyses()
            assertThat(allAnalyses).hasSize(1)
            assertThat(allAnalyses[0].userId).isEqualTo("user-1")
        }

    @Test
    fun `deleteAllAnalyses should remove all analyses`() =
        runTest {
            val analysis1 = createAnalysis(id = "analysis-1")
            val analysis2 = createAnalysis(id = "analysis-2")
            val analysis3 = createAnalysis(id = "analysis-3")

            trainingAnalysisDao.insertAnalysis(analysis1)
            trainingAnalysisDao.insertAnalysis(analysis2)
            trainingAnalysisDao.insertAnalysis(analysis3)

            trainingAnalysisDao.deleteAllAnalyses()

            val allAnalyses = trainingAnalysisDao.getAllAnalyses()
            assertThat(allAnalyses).isEmpty()
        }

    // JSON Field Handling Tests

    @Test
    fun `should persist and retrieve keyInsightsJson correctly`() =
        runTest {
            val insightsJson =
                """[{"category":"VOLUME","message":"Good volume","severity":"SUCCESS"}]"""
            val analysis = createAnalysis(keyInsightsJson = insightsJson)

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.keyInsightsJson).isEqualTo(insightsJson)
        }

    @Test
    fun `should persist and retrieve recommendationsJson correctly`() =
        runTest {
            val recommendationsJson = """["Increase volume","Focus on recovery"]"""
            val analysis = createAnalysis(recommendationsJson = recommendationsJson)

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.recommendationsJson).isEqualTo(recommendationsJson)
        }

    @Test
    fun `should persist and retrieve warningsJson correctly`() =
        runTest {
            val warningsJson = """["High fatigue detected","Missing workouts"]"""
            val analysis = createAnalysis(warningsJson = warningsJson)

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.warningsJson).isEqualTo(warningsJson)
        }

    @Test
    fun `should persist and retrieve adherenceAnalysisJson correctly`() =
        runTest {
            val adherenceJson =
                """{"adherenceScore":85,"scoreExplanation":"Good adherence","positivePatterns":[],"negativePatterns":[],"adherenceRecommendations":[]}"""
            val analysis = createAnalysis(adherenceAnalysisJson = adherenceJson)

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.adherenceAnalysisJson).isEqualTo(adherenceJson)
        }

    @Test
    fun `should handle null adherenceAnalysisJson`() =
        runTest {
            val analysis = createAnalysis(adherenceAnalysisJson = null)

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.adherenceAnalysisJson).isNull()
        }

    @Test
    fun `should handle empty JSON arrays`() =
        runTest {
            val analysis =
                createAnalysis(
                    keyInsightsJson = "[]",
                    recommendationsJson = "[]",
                    warningsJson = "[]",
                )

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.keyInsightsJson).isEqualTo("[]")
            assertThat(retrieved?.recommendationsJson).isEqualTo("[]")
            assertThat(retrieved?.warningsJson).isEqualTo("[]")
        }

    // Date Field Tests

    @Test
    fun `should persist and retrieve date fields correctly`() =
        runTest {
            val analysisDate = LocalDateTime.of(2025, 6, 15, 10, 30)
            val periodStart = LocalDate.of(2025, 6, 1)
            val periodEnd = LocalDate.of(2025, 6, 14)

            val analysis =
                createAnalysis(
                    analysisDate = analysisDate,
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                )

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.analysisDate).isEqualTo(analysisDate)
            assertThat(retrieved?.periodStart).isEqualTo(periodStart)
            assertThat(retrieved?.periodEnd).isEqualTo(periodEnd)
        }

    @Test
    fun `should handle analyses spanning multiple weeks`() =
        runTest {
            val periodStart = LocalDate.of(2025, 1, 1)
            val periodEnd = LocalDate.of(2025, 1, 31)

            val analysis =
                createAnalysis(
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    overallAssessment = "Monthly analysis",
                )

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.periodStart).isEqualTo(periodStart)
            assertThat(retrieved?.periodEnd).isEqualTo(periodEnd)
        }

    // Edge Cases and Special Scenarios

    @Test
    fun `should handle long overallAssessment text`() =
        runTest {
            val longAssessment = "This is a very long assessment. ".repeat(100)
            val analysis = createAnalysis(overallAssessment = longAssessment)

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.overallAssessment).isEqualTo(longAssessment)
        }

    @Test
    fun `should handle special characters in JSON fields`() =
        runTest {
            val jsonWithSpecialChars = """["Don't skip rest days","Use 90° angle"]"""
            val analysis = createAnalysis(recommendationsJson = jsonWithSpecialChars)

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.recommendationsJson).isEqualTo(jsonWithSpecialChars)
        }

    @Test
    fun `should handle unicode characters in assessment`() =
        runTest {
            val unicodeAssessment = "训练进展良好"
            val analysis = createAnalysis(overallAssessment = unicodeAssessment)

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.overallAssessment).isEqualTo(unicodeAssessment)
        }

    @Test
    fun `should maintain analysis ordering by date`() =
        runTest {
            val baseDate = LocalDateTime.of(2025, 6, 1, 12, 0)

            val analysis1 = createAnalysis(id = "analysis-1", analysisDate = baseDate.plusDays(3))
            val analysis2 = createAnalysis(id = "analysis-2", analysisDate = baseDate.plusDays(1))
            val analysis3 = createAnalysis(id = "analysis-3", analysisDate = baseDate.plusDays(5))

            trainingAnalysisDao.insertAnalysis(analysis1)
            trainingAnalysisDao.insertAnalysis(analysis2)
            trainingAnalysisDao.insertAnalysis(analysis3)

            val latest = trainingAnalysisDao.getLatestAnalysis()

            assertThat(latest?.id).isEqualTo("analysis-3")
        }

    @Test
    fun `should handle complex JSON structures`() =
        runTest {
            val complexInsightsJson =
                """[
                    {"category":"VOLUME","message":"Volume increased by 15%","severity":"SUCCESS"},
                    {"category":"INTENSITY","message":"Intensity stable","severity":"INFO"},
                    {"category":"RECOVERY","message":"Consider deload week","severity":"WARNING"}
                ]"""

            val analysis = createAnalysis(keyInsightsJson = complexInsightsJson)

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved?.keyInsightsJson).isEqualTo(complexInsightsJson)
        }

    @Test
    fun `should handle analysis with all optional fields populated`() =
        runTest {
            val analysis =
                createAnalysis(
                    userId = "user-1",
                    overallAssessment = "Excellent progress",
                    keyInsightsJson = """[{"category":"VOLUME","message":"Good","severity":"SUCCESS"}]""",
                    recommendationsJson = """["Continue current program"]""",
                    warningsJson = """["Watch for overtraining"]""",
                    adherenceAnalysisJson = """{"adherenceScore":90,"scoreExplanation":"Great","positivePatterns":[],"negativePatterns":[],"adherenceRecommendations":[]}""",
                )

            trainingAnalysisDao.insertAnalysis(analysis)

            val retrieved = trainingAnalysisDao.getAnalysisById(analysis.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.userId).isEqualTo("user-1")
            assertThat(retrieved?.overallAssessment).isEqualTo("Excellent progress")
            assertThat(retrieved?.adherenceAnalysisJson).isNotNull()
        }
}
