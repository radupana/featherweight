package com.github.radupana.featherweight

import com.google.common.truth.Truth.assertThat
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.junit.Test

class FeatherweightApplicationTest {
    private val application = FeatherweightApplication()

    @Test
    fun `getAppCheckProviderFactory returns Debug provider for debug build`() {
        val provider = application.getAppCheckProviderFactory("debug")

        assertThat(provider).isInstanceOf(DebugAppCheckProviderFactory::class.java)
    }

    @Test
    fun `getAppCheckProviderFactory returns Play Integrity provider for alpha build`() {
        val provider = application.getAppCheckProviderFactory("alpha")

        assertThat(provider).isInstanceOf(PlayIntegrityAppCheckProviderFactory::class.java)
    }

    @Test
    fun `getAppCheckProviderFactory returns Play Integrity provider for release build`() {
        val provider = application.getAppCheckProviderFactory("release")

        assertThat(provider).isInstanceOf(PlayIntegrityAppCheckProviderFactory::class.java)
    }

    @Test
    fun `getAppCheckProviderFactory returns Play Integrity provider for unknown build type`() {
        val provider = application.getAppCheckProviderFactory("unknown")

        assertThat(provider).isInstanceOf(PlayIntegrityAppCheckProviderFactory::class.java)
    }
}
