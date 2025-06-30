package com.jjordanoc.yachai.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jjordanoc.yachai.utils.FileUtils
import com.jjordanoc.yachai.utils.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.net.URL

sealed interface DownloadState {
    object Idle : DownloadState
    object Downloading : DownloadState
    object Success : DownloadState
    data class Error(val message: String) : DownloadState
}

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    fun downloadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _downloadState.value = DownloadState.Downloading
            try {
                val modelUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task"
                Log.d(TAG, "Starting model download from: $modelUrl")
                val context = getApplication<Application>().applicationContext
                val modelFile = FileUtils.getModelFile(context)
                Log.d(TAG, "Saving model to: ${modelFile.absolutePath}")

                URL(modelUrl).openStream().use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
                _downloadState.value = DownloadState.Success
                Log.d(TAG, "Model download successful.")
            } catch (e: Exception) {
                Log.e(TAG, "Model download failed: ${e.stackTraceToString()}")
                _downloadState.value = DownloadState.Error("Error en la descarga: ${e.localizedMessage}")
            }
        }
    }
} 