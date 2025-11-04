package com.github.radupana.featherweight.security

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class DatabaseKeyManagerTest {
    @Test
    fun getDatabasePassphrase_returnsNonEmptyByteArray() {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context

        val keyManager = DatabaseKeyManager(context)
        val passphrase = keyManager.getDatabasePassphrase()

        assertThat(passphrase).isNotEmpty()
    }

    @Test
    fun getDatabasePassphrase_returnsSameKeyOnMultipleCalls() {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context

        val keyManager = DatabaseKeyManager(context)
        val passphrase1 = keyManager.getDatabasePassphrase()
        val passphrase2 = keyManager.getDatabasePassphrase()

        assertThat(passphrase1).isEqualTo(passphrase2)
    }

    @Test
    fun getDatabasePassphrase_generatesMinimumLengthKey() {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context

        val keyManager = DatabaseKeyManager(context)
        val passphrase = keyManager.getDatabasePassphrase()

        assertThat(passphrase.size).isAtLeast(32)
    }

    @Test
    fun clearDatabaseKey_allowsNewKeyGeneration() {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context

        val keyManager = DatabaseKeyManager(context)
        val originalPassphrase = keyManager.getDatabasePassphrase()

        keyManager.clearDatabaseKey()

        val newPassphrase = keyManager.getDatabasePassphrase()
        assertThat(newPassphrase).isNotEmpty()
        assertThat(newPassphrase).isNotEqualTo(originalPassphrase)
    }
}
