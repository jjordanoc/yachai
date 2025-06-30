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
import java.net.HttpURLConnection
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
            var connection: HttpURLConnection? = null
            try {
                // IMPORTANT: Replace with your actual Hugging Face token.
                val authToken = "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"
                val modelUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task"
                Log.d(TAG, "Starting model download from: $modelUrl")
                val context = getApplication<Application>().applicationContext
                val modelFile = FileUtils.getModelFile(context)
                Log.d(TAG, "Saving model to: ${modelFile.absolutePath}")

                val url = URL(modelUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.instanceFollowRedirects = true
                connection.connect()

                val responseCode = connection.responseCode
                Log.d(TAG, "HTTP Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input ->
                        FileOutputStream(modelFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    _downloadState.value = DownloadState.Success
                    Log.d(TAG, "Model download successful.")
                } else {
                    val errorMsg = "Download failed with HTTP status: $responseCode ${connection.responseMessage}"
                    Log.e(TAG, errorMsg)
                    _downloadState.value = DownloadState.Error(errorMsg)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Model download failed: ${e.stackTraceToString()}")
                _downloadState.value = DownloadState.Error("Error en la descarga: ${e.localizedMessage}")
            } finally {
                connection?.disconnect()
            }
        }
    }
} 