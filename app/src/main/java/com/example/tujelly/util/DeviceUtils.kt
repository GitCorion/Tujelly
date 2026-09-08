package com.example.tujelly.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build

object DeviceUtils {
    fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT?.lowercase() ?: ""
        val model = Build.MODEL?.lowercase() ?: ""
        val brand = Build.BRAND?.lowercase() ?: ""
        val device = Build.DEVICE?.lowercase() ?: ""
        val product = Build.PRODUCT?.lowercase() ?: ""
        val hardware = Build.HARDWARE?.lowercase() ?: ""
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""

        return (brand.startsWith("generic") && device.startsWith("generic"))
                || fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for")
                || manufacturer.contains("genymotion")
                || product.contains("sdk_google")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("vbox86p")
                || product.contains("simulator")
    }

    /**
     * Detecta automáticamente si el dispositivo es de bajos recursos (RAM baja, POCOs núcleos o bandera isLowRamDevice).
     */
    fun isLowResourceDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (activityManager?.isLowRamDevice == true) {
            return true
        }

        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalMemMb = memInfo.totalMem / (1024 * 1024)

        val cpuCores = Runtime.getRuntime().availableProcessors()

        // Dispositivos TV con menos de 2.5GB de RAM o <= 2 núcleos se consideran de recursos limitados
        return totalMemMb < 2560 || cpuCores <= 2
    }

    /**
     * Retorna un factor de multiplicación (0.25f a 1.0f) para escalar la densidad de partículas y efectos visuales.
     */
    fun getRecommendedParticleScale(context: Context): Float {
        return if (isLowResourceDevice(context)) 0.3f else 1.0f
    }
}

