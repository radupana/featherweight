package com.github.radupana.featherweight.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class InstallationIdProviderTest {
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        // Clear the cached installation ID before each test
        InstallationIdProvider.clearCache()

        // Set up mocks
        mockContext = mockk()
        mockSharedPreferences = mockk()
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
    }

    @Test
    fun `getId generates new UUID when no existing ID`() {
        // Given: No existing installation ID
        every { mockSharedPreferences.getString("installation_id", null) } returns null

        val savedIdSlot = slot<String>()
        every { mockEditor.putString("installation_id", capture(savedIdSlot)) } returns mockEditor

        // When: Getting the installation ID
        val id = InstallationIdProvider.getId(mockContext)

        // Then: A new UUID is generated and saved
        assertNotNull(id)
        assertEquals(36, id.length) // UUID string length
        verify { mockEditor.putString("installation_id", savedIdSlot.captured) }
        verify { mockEditor.apply() }
        assertEquals(id, savedIdSlot.captured)

        // Verify it's a valid UUID
        val uuid = UUID.fromString(id)
        assertNotNull(uuid)
    }

    @Test
    fun `getId returns existing ID when already stored`() {
        // Given: An existing installation ID
        val existingId = UUID.randomUUID().toString()
        every { mockSharedPreferences.getString("installation_id", null) } returns existingId

        // When: Getting the installation ID
        val id = InstallationIdProvider.getId(mockContext)

        // Then: The existing ID is returned
        assertEquals(existingId, id)
        verify(exactly = 0) { mockEditor.putString(any(), any()) }
    }

    @Test
    fun `getId uses cached value on subsequent calls`() {
        // Given: No existing installation ID on first call
        every { mockSharedPreferences.getString("installation_id", null) } returns null

        val savedIdSlot = slot<String>()
        every { mockEditor.putString("installation_id", capture(savedIdSlot)) } returns mockEditor

        // When: Getting the installation ID multiple times
        val id1 = InstallationIdProvider.getId(mockContext)
        val id2 = InstallationIdProvider.getId(mockContext)
        val id3 = InstallationIdProvider.getId(mockContext)

        // Then: The same ID is returned and SharedPreferences is only accessed once
        assertEquals(id1, id2)
        assertEquals(id2, id3)
        verify(exactly = 1) { mockSharedPreferences.getString("installation_id", null) }
        verify(exactly = 1) { mockEditor.putString("installation_id", any()) }
    }

    @Test
    fun `clearCache forces new read from SharedPreferences`() {
        // Given: An existing installation ID
        val existingId = UUID.randomUUID().toString()
        every { mockSharedPreferences.getString("installation_id", null) } returns existingId

        // When: Getting ID, clearing cache, and getting ID again
        val id1 = InstallationIdProvider.getId(mockContext)
        InstallationIdProvider.clearCache()
        val id2 = InstallationIdProvider.getId(mockContext)

        // Then: SharedPreferences is accessed twice
        assertEquals(id1, id2)
        assertEquals(existingId, id1)
        verify(exactly = 2) { mockSharedPreferences.getString("installation_id", null) }
    }

    @Test
    fun `concurrent access returns same ID`() {
        // Given: No existing installation ID
        every { mockSharedPreferences.getString("installation_id", null) } returns null

        val savedIdSlot = slot<String>()
        every { mockEditor.putString("installation_id", capture(savedIdSlot)) } returns mockEditor

        // When: Multiple threads try to get the ID simultaneously
        val ids = CopyOnWriteArrayList<String>()
        val threads =
            (1..10).map {
                Thread {
                    ids.add(InstallationIdProvider.getId(mockContext))
                }
            }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Then: All threads get the same ID
        assertEquals(10, ids.size)
        val firstId = ids.first()
        ids.forEach { assertEquals(firstId, it) }

        // And SharedPreferences is written only once
        verify(exactly = 1) { mockEditor.putString("installation_id", any()) }
    }

    @Test
    fun `different contexts still use same cached value`() {
        // Given: Two different contexts
        val mockContext2 = mockk<Context>()
        val mockSharedPreferences2 = mockk<SharedPreferences>()

        every { mockContext2.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) } returns mockSharedPreferences2

        // First context has no existing ID
        every { mockSharedPreferences.getString("installation_id", null) } returns null
        val savedIdSlot = slot<String>()
        every { mockEditor.putString("installation_id", capture(savedIdSlot)) } returns mockEditor

        // When: Getting ID with first context, then with second context
        val id1 = InstallationIdProvider.getId(mockContext)
        val id2 = InstallationIdProvider.getId(mockContext2)

        // Then: Second context gets the cached value without accessing SharedPreferences
        assertEquals(id1, id2)
        verify(exactly = 0) { mockSharedPreferences2.getString(any(), any()) }
    }
}
