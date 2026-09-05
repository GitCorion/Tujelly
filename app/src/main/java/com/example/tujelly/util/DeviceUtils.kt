package com.example.tujelly.util

import android.os.Build

object DeviceUtils {
    fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()

        return (brand.startsWith("generic") && device.startsWith("generic"))
                || fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for")
                || Build.MANUFACTURER.lowercase().contains("genymotion")
                || product.contains("sdk_google")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("vbox86p")
                || product.contains("simulator")
    }
}
