package com.example.tujelly.util

import org.junit.Assert.assertNotNull
import org.junit.Test

class DeviceUtilsTest {

    @Test
    fun `isEmulator evaluates build attributes without throwing exceptions`() {
        val result = DeviceUtils.isEmulator()
        assertNotNull(result)
    }
}
