package com.github.radupana.featherweight.util

import java.util.UUID

object IdGenerator {
    fun generateId(): String = UUID.randomUUID().toString()
}
