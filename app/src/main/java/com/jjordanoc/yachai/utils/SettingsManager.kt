package com.jjordanoc.yachai.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SettingsManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("Yachai_Settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_GPU_ENABLED = "gpu_enabled"
    }

    fun isGpuEnabled(): Boolean {
        // Default to true (GPU enabled) if no preference is set yet
        val useGpu = sharedPreferences.getBoolean(KEY_GPU_ENABLED, true)
        Log.d(TAG, "GPU enabled: $useGpu")
        return useGpu
    }

    fun setGpuEnabled(isEnabled: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(KEY_GPU_ENABLED, isEnabled)
            apply()
        }
    }
} 