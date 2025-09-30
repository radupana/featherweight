package com.github.radupana.featherweight.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class IdGeneratorTest {
    @Test
    fun `generateId creates valid UUID string`() {
        val id = IdGenerator.generateId()

        assertNotNull(id)
        assertTrue(id.isNotEmpty())
        val uuid = UUID.fromString(id)
        assertNotNull(uuid)
    }

    @Test
    fun `generateId creates unique IDs`() {
        val id1 = IdGenerator.generateId()
        val id2 = IdGenerator.generateId()
        val id3 = IdGenerator.generateId()

        assertNotEquals(id1, id2)
        assertNotEquals(id2, id3)
        assertNotEquals(id1, id3)
    }

    @Test
    fun `generated ID has expected UUID format`() {
        val id = IdGenerator.generateId()

        val parts = id.split("-")
        assertEquals(5, parts.size)
        assertEquals(8, parts[0].length)
        assertEquals(4, parts[1].length)
        assertEquals(4, parts[2].length)
        assertEquals(4, parts[3].length)
        assertEquals(12, parts[4].length)
    }

    @Test
    fun `generated IDs work as database keys`() {
        val ids = mutableSetOf<String>()

        repeat(1000) {
            ids.add(IdGenerator.generateId())
        }

        assertEquals(1000, ids.size)
    }

    @Test
    fun `generated IDs are compatible with SQLite and Firestore`() {
        val id = IdGenerator.generateId()

        assertTrue(id.length <= 36)
        assertTrue(id.matches(Regex("[a-f0-9-]+")))

        val testMap = mapOf(id to "test")
        assertEquals("test", testMap[id])
    }
}
