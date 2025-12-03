package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

/**
 * Test suite for ParseRequestDao.
 *
 * Tests all DAO methods including:
 * - CRUD operations
 * - Status-based queries
 * - Flow-based observables
 * - Timestamp handling
 * - User filtering
 */
class ParseRequestDaoTest : BaseDaoTest() {
    // Helper Methods

    /**
     * Creates a test ParseRequest.
     */
    private fun createParseRequest(
        id: String = "test-request",
        userId: String? = "test-user",
        rawText: String = "Test workout text",
        createdAt: LocalDateTime = LocalDateTime.now(),
        status: ParseStatus = ParseStatus.PROCESSING,
        error: String? = null,
        resultJson: String? = null,
        completedAt: LocalDateTime? = null,
    ): ParseRequest =
        ParseRequest(
            id = id,
            userId = userId,
            rawText = rawText,
            createdAt = createdAt,
            status = status,
            error = error,
            resultJson = resultJson,
            completedAt = completedAt,
        )

    // CRUD Operations Tests

    @Test
    fun `insert should add request to database`() =
        runTest {
            val request = createParseRequest()

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(request.id)
            assertThat(retrieved?.rawText).isEqualTo("Test workout text")
            assertThat(retrieved?.status).isEqualTo(ParseStatus.PROCESSING)
        }

    @Test
    fun `insertParseRequest should add request to database`() =
        runTest {
            val request = createParseRequest()

            parseRequestDao.insertParseRequest(request)

            val retrieved = parseRequestDao.getParseRequestById(request.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(request.id)
        }

    @Test
    fun `update should modify existing request`() =
        runTest {
            val request = createParseRequest(status = ParseStatus.PROCESSING)

            parseRequestDao.insert(request)

            val updated = request.copy(status = ParseStatus.COMPLETED, completedAt = LocalDateTime.now())
            parseRequestDao.update(updated)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.status).isEqualTo(ParseStatus.COMPLETED)
            assertThat(retrieved?.completedAt).isNotNull()
        }

    @Test
    fun `update should not affect other requests`() =
        runTest {
            val request1 = createParseRequest(id = "request-1", rawText = "Text 1")
            val request2 = createParseRequest(id = "request-2", rawText = "Text 2")

            parseRequestDao.insert(request1)
            parseRequestDao.insert(request2)

            val updated1 = request1.copy(rawText = "Updated Text 1")
            parseRequestDao.update(updated1)

            val retrieved2 = parseRequestDao.getRequest(request2.id)
            assertThat(retrieved2?.rawText).isEqualTo("Text 2")
        }

    @Test
    fun `getRequest should return specific request`() =
        runTest {
            val request = createParseRequest()

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)

            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(request.id)
            assertThat(retrieved?.rawText).isEqualTo("Test workout text")
        }

    @Test
    fun `getRequest should return null for non-existent id`() =
        runTest {
            val retrieved = parseRequestDao.getRequest("non-existent-id")

            assertThat(retrieved).isNull()
        }

    @Test
    fun `getParseRequestById should return specific request`() =
        runTest {
            val request = createParseRequest()

            parseRequestDao.insertParseRequest(request)

            val retrieved = parseRequestDao.getParseRequestById(request.id)

            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(request.id)
        }

    @Test
    fun `getParseRequestById should return null for non-existent id`() =
        runTest {
            val retrieved = parseRequestDao.getParseRequestById("non-existent-id")

            assertThat(retrieved).isNull()
        }

    // Query Operations Tests

    @Test
    fun `getAllRequests should return flow of all requests ordered by date desc`() =
        runTest {
            val baseTime = LocalDateTime.of(2025, 6, 1, 12, 0)

            val request1 = createParseRequest(id = "request-1", createdAt = baseTime)
            val request2 = createParseRequest(id = "request-2", createdAt = baseTime.plusHours(2))
            val request3 = createParseRequest(id = "request-3", createdAt = baseTime.plusHours(1))

            parseRequestDao.insert(request1)
            parseRequestDao.insert(request2)
            parseRequestDao.insert(request3)

            val requests = parseRequestDao.getAllRequests().first()

            assertThat(requests).hasSize(3)
            assertThat(requests.map { it.id }).containsExactly("request-2", "request-3", "request-1").inOrder()
        }

    @Test
    fun `getAllRequests flow should emit updated list after insert`() =
        runTest {
            val request1 = createParseRequest(id = "request-1")
            parseRequestDao.insert(request1)

            val initialRequests = parseRequestDao.getAllRequests().first()
            assertThat(initialRequests).hasSize(1)

            val request2 = createParseRequest(id = "request-2")
            parseRequestDao.insert(request2)

            val updatedRequests = parseRequestDao.getAllRequests().first()
            assertThat(updatedRequests).hasSize(2)
        }

    @Test
    fun `getAllRequestsList should return all requests`() =
        runTest {
            val request1 = createParseRequest(id = "request-1")
            val request2 = createParseRequest(id = "request-2")
            val request3 = createParseRequest(id = "request-3")

            parseRequestDao.insert(request1)
            parseRequestDao.insert(request2)
            parseRequestDao.insert(request3)

            val requests = parseRequestDao.getAllRequestsList()

            assertThat(requests).hasSize(3)
            assertThat(requests.map { it.id }).containsExactly("request-1", "request-2", "request-3")
        }

    @Test
    fun `getPendingRequest should return most recent PROCESSING or COMPLETED request`() =
        runTest {
            val baseTime = LocalDateTime.of(2025, 6, 1, 12, 0)

            val failedRequest =
                createParseRequest(
                    id = "failed",
                    status = ParseStatus.FAILED,
                    createdAt = baseTime.plusHours(3),
                )
            val processingRequest =
                createParseRequest(
                    id = "processing",
                    status = ParseStatus.PROCESSING,
                    createdAt = baseTime.plusHours(2),
                )
            val completedRequest =
                createParseRequest(
                    id = "completed",
                    status = ParseStatus.COMPLETED,
                    createdAt = baseTime.plusHours(4),
                )
            val importedRequest =
                createParseRequest(
                    id = "imported",
                    status = ParseStatus.IMPORTED,
                    createdAt = baseTime.plusHours(5),
                )

            parseRequestDao.insert(failedRequest)
            parseRequestDao.insert(processingRequest)
            parseRequestDao.insert(completedRequest)
            parseRequestDao.insert(importedRequest)

            val pending = parseRequestDao.getPendingRequest()

            assertThat(pending).isNotNull()
            assertThat(pending?.id).isEqualTo("completed")
        }

    @Test
    fun `getPendingRequest should return null when no pending requests exist`() =
        runTest {
            val failedRequest = createParseRequest(id = "failed-request", status = ParseStatus.FAILED)
            val importedRequest = createParseRequest(id = "imported-request", status = ParseStatus.IMPORTED)

            parseRequestDao.insert(failedRequest)
            parseRequestDao.insert(importedRequest)

            val pending = parseRequestDao.getPendingRequest()

            assertThat(pending).isNull()
        }

    @Test
    fun `getPendingRequest should prioritize most recent by creation date`() =
        runTest {
            val baseTime = LocalDateTime.of(2025, 6, 1, 12, 0)

            val older = createParseRequest(id = "older", status = ParseStatus.PROCESSING, createdAt = baseTime)
            val newer =
                createParseRequest(
                    id = "newer",
                    status = ParseStatus.COMPLETED,
                    createdAt = baseTime.plusHours(1),
                )

            parseRequestDao.insert(older)
            parseRequestDao.insert(newer)

            val pending = parseRequestDao.getPendingRequest()

            assertThat(pending?.id).isEqualTo("newer")
        }

    @Test
    fun `getPendingRequestCount should return count of PROCESSING and COMPLETED requests`() =
        runTest {
            val processing1 = createParseRequest(id = "processing-1", status = ParseStatus.PROCESSING)
            val processing2 = createParseRequest(id = "processing-2", status = ParseStatus.PROCESSING)
            val completed = createParseRequest(id = "completed", status = ParseStatus.COMPLETED)
            val failed = createParseRequest(id = "failed", status = ParseStatus.FAILED)
            val imported = createParseRequest(id = "imported", status = ParseStatus.IMPORTED)

            parseRequestDao.insert(processing1)
            parseRequestDao.insert(processing2)
            parseRequestDao.insert(completed)
            parseRequestDao.insert(failed)
            parseRequestDao.insert(imported)

            val count = parseRequestDao.getPendingRequestCount()

            assertThat(count).isEqualTo(3)
        }

    @Test
    fun `getPendingRequestCount should return 0 when no pending requests exist`() =
        runTest {
            val failed = createParseRequest(id = "failed-request", status = ParseStatus.FAILED)
            val imported = createParseRequest(id = "imported-request", status = ParseStatus.IMPORTED)

            parseRequestDao.insert(failed)
            parseRequestDao.insert(imported)

            val count = parseRequestDao.getPendingRequestCount()

            assertThat(count).isEqualTo(0)
        }

    // Deletion Operations Tests

    @Test
    fun `deleteById should remove specific request`() =
        runTest {
            val request1 = createParseRequest(id = "request-1")
            val request2 = createParseRequest(id = "request-2")

            parseRequestDao.insert(request1)
            parseRequestDao.insert(request2)

            parseRequestDao.deleteById(request1.id)

            val retrieved1 = parseRequestDao.getRequest(request1.id)
            val retrieved2 = parseRequestDao.getRequest(request2.id)

            assertThat(retrieved1).isNull()
            assertThat(retrieved2).isNotNull()
        }

    @Test
    fun `deleteAllForUser should remove all requests for specific user`() =
        runTest {
            val user1Request1 = createParseRequest(id = "user1-request1", userId = "user-1")
            val user1Request2 = createParseRequest(id = "user1-request2", userId = "user-1")
            val user2Request = createParseRequest(id = "user2-request", userId = "user-2")

            parseRequestDao.insert(user1Request1)
            parseRequestDao.insert(user1Request2)
            parseRequestDao.insert(user2Request)

            parseRequestDao.deleteAllForUser("user-1")

            val allRequests = parseRequestDao.getAllRequestsList()
            assertThat(allRequests).hasSize(1)
            assertThat(allRequests[0].userId).isEqualTo("user-2")
        }

    @Test
    fun `deleteAllForUser should not affect other users`() =
        runTest {
            val user1Request = createParseRequest(id = "user1-request", userId = "user-1")
            val user2Request = createParseRequest(id = "user2-request", userId = "user-2")

            parseRequestDao.insert(user1Request)
            parseRequestDao.insert(user2Request)

            parseRequestDao.deleteAllForUser("user-1")

            val retrieved = parseRequestDao.getRequest("user2-request")
            assertThat(retrieved).isNotNull()
        }

    @Test
    fun `deleteAllWhereUserIdIsNull should remove requests with null userId`() =
        runTest {
            val requestWithUser = createParseRequest(id = "with-user", userId = "user-1")
            val requestWithoutUser1 = createParseRequest(id = "without-user-1", userId = null)
            val requestWithoutUser2 = createParseRequest(id = "without-user-2", userId = null)

            parseRequestDao.insert(requestWithUser)
            parseRequestDao.insert(requestWithoutUser1)
            parseRequestDao.insert(requestWithoutUser2)

            parseRequestDao.deleteAllWhereUserIdIsNull()

            val allRequests = parseRequestDao.getAllRequestsList()
            assertThat(allRequests).hasSize(1)
            assertThat(allRequests[0].id).isEqualTo("with-user")
        }

    @Test
    fun `deleteAllWhereUserIdIsNull should not affect requests with userId`() =
        runTest {
            val requestWithUser = createParseRequest(id = "with-user", userId = "user-1")
            val requestWithoutUser = createParseRequest(id = "without-user", userId = null)

            parseRequestDao.insert(requestWithUser)
            parseRequestDao.insert(requestWithoutUser)

            parseRequestDao.deleteAllWhereUserIdIsNull()

            val allRequests = parseRequestDao.getAllRequestsList()
            assertThat(allRequests).hasSize(1)
            assertThat(allRequests[0].userId).isEqualTo("user-1")
        }

    @Test
    fun `deleteAllRequests should remove all requests`() =
        runTest {
            val request1 = createParseRequest(id = "request-1")
            val request2 = createParseRequest(id = "request-2")
            val request3 = createParseRequest(id = "request-3")

            parseRequestDao.insert(request1)
            parseRequestDao.insert(request2)
            parseRequestDao.insert(request3)

            parseRequestDao.deleteAllRequests()

            val allRequests = parseRequestDao.getAllRequestsList()
            assertThat(allRequests).isEmpty()
        }

    // Status Handling Tests

    @Test
    fun `should persist all ParseStatus enum values correctly`() =
        runTest {
            val processingRequest = createParseRequest(id = "processing", status = ParseStatus.PROCESSING)
            val completedRequest = createParseRequest(id = "completed", status = ParseStatus.COMPLETED)
            val failedRequest = createParseRequest(id = "failed", status = ParseStatus.FAILED)
            val importedRequest = createParseRequest(id = "imported", status = ParseStatus.IMPORTED)

            parseRequestDao.insert(processingRequest)
            parseRequestDao.insert(completedRequest)
            parseRequestDao.insert(failedRequest)
            parseRequestDao.insert(importedRequest)

            assertThat(parseRequestDao.getRequest("processing")?.status).isEqualTo(ParseStatus.PROCESSING)
            assertThat(parseRequestDao.getRequest("completed")?.status).isEqualTo(ParseStatus.COMPLETED)
            assertThat(parseRequestDao.getRequest("failed")?.status).isEqualTo(ParseStatus.FAILED)
            assertThat(parseRequestDao.getRequest("imported")?.status).isEqualTo(ParseStatus.IMPORTED)
        }

    @Test
    fun `should update status from PROCESSING to COMPLETED`() =
        runTest {
            val request = createParseRequest(status = ParseStatus.PROCESSING)

            parseRequestDao.insert(request)

            val updated = request.copy(status = ParseStatus.COMPLETED, completedAt = LocalDateTime.now())
            parseRequestDao.update(updated)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.status).isEqualTo(ParseStatus.COMPLETED)
        }

    @Test
    fun `should update status from PROCESSING to FAILED with error message`() =
        runTest {
            val request = createParseRequest(status = ParseStatus.PROCESSING)

            parseRequestDao.insert(request)

            val updated = request.copy(status = ParseStatus.FAILED, error = "Parse error occurred")
            parseRequestDao.update(updated)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.status).isEqualTo(ParseStatus.FAILED)
            assertThat(retrieved?.error).isEqualTo("Parse error occurred")
        }

    @Test
    fun `should update status from COMPLETED to IMPORTED`() =
        runTest {
            val request = createParseRequest(status = ParseStatus.COMPLETED)

            parseRequestDao.insert(request)

            val updated = request.copy(status = ParseStatus.IMPORTED)
            parseRequestDao.update(updated)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.status).isEqualTo(ParseStatus.IMPORTED)
        }

    // Timestamp Handling Tests

    @Test
    fun `should persist createdAt timestamp correctly`() =
        runTest {
            val timestamp = LocalDateTime.of(2025, 6, 15, 14, 30, 45)
            val request = createParseRequest(createdAt = timestamp)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.createdAt).isEqualTo(timestamp)
        }

    @Test
    fun `should persist completedAt timestamp correctly`() =
        runTest {
            val completedTime = LocalDateTime.of(2025, 6, 15, 15, 0, 0)
            val request = createParseRequest(status = ParseStatus.COMPLETED, completedAt = completedTime)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.completedAt).isEqualTo(completedTime)
        }

    @Test
    fun `should handle null completedAt for non-completed requests`() =
        runTest {
            val request = createParseRequest(status = ParseStatus.PROCESSING, completedAt = null)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.completedAt).isNull()
        }

    // JSON and Error Field Tests

    @Test
    fun `should persist resultJson correctly`() =
        runTest {
            val resultJson = """{"programme":"Test Programme","weeks":4}"""
            val request = createParseRequest(status = ParseStatus.COMPLETED, resultJson = resultJson)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.resultJson).isEqualTo(resultJson)
        }

    @Test
    fun `should handle null resultJson`() =
        runTest {
            val request = createParseRequest(resultJson = null)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.resultJson).isNull()
        }

    @Test
    fun `should persist error message correctly`() =
        runTest {
            val errorMessage = "Failed to parse workout text"
            val request = createParseRequest(status = ParseStatus.FAILED, error = errorMessage)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.error).isEqualTo(errorMessage)
        }

    @Test
    fun `should handle null error for successful requests`() =
        runTest {
            val request = createParseRequest(status = ParseStatus.COMPLETED, error = null)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.error).isNull()
        }

    // Edge Cases and Special Scenarios

    @Test
    fun `should handle long rawText content`() =
        runTest {
            val longText = "Week 1: Squat 5x5. ".repeat(100)
            val request = createParseRequest(rawText = longText)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.rawText).isEqualTo(longText)
        }

    @Test
    fun `should handle special characters in rawText`() =
        runTest {
            val specialText = "Day 1: Bench Press @ 85% 1RM, 3x5. Don't forget rest!"
            val request = createParseRequest(rawText = specialText)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.rawText).isEqualTo(specialText)
        }

    @Test
    fun `should handle unicode characters in rawText`() =
        runTest {
            val unicodeText = "训练计划"
            val request = createParseRequest(rawText = unicodeText)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.rawText).isEqualTo(unicodeText)
        }

    @Test
    fun `should handle complex resultJson`() =
        runTest {
            val complexJson =
                """
                {
                    "programme": "Advanced Programme",
                    "weeks": 8,
                    "exercises": [
                        {"name": "Squat", "sets": 5, "reps": 5},
                        {"name": "Bench Press", "sets": 3, "reps": 8}
                    ]
                }
                """.trimIndent()

            val request = createParseRequest(resultJson = complexJson)

            parseRequestDao.insert(request)

            val retrieved = parseRequestDao.getRequest(request.id)
            assertThat(retrieved?.resultJson).isEqualTo(complexJson)
        }

    @Test
    fun `should maintain request ordering across multiple operations`() =
        runTest {
            val baseTime = LocalDateTime.of(2025, 6, 1, 12, 0)

            val request1 = createParseRequest(id = "request-1", createdAt = baseTime)
            val request2 = createParseRequest(id = "request-2", createdAt = baseTime.plusHours(2))
            val request3 = createParseRequest(id = "request-3", createdAt = baseTime.plusHours(1))

            parseRequestDao.insert(request1)
            parseRequestDao.insert(request2)
            parseRequestDao.insert(request3)

            val updated2 = request2.copy(status = ParseStatus.COMPLETED)
            parseRequestDao.update(updated2)

            val requests = parseRequestDao.getAllRequests().first()

            assertThat(requests.map { it.id }).containsExactly("request-2", "request-3", "request-1").inOrder()
        }
}
