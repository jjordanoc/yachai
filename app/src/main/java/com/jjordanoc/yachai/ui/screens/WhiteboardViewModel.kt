package com.jjordanoc.yachai.ui.screens

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.jjordanoc.yachai.data.*
import com.jjordanoc.yachai.utils.TAG
import com.jjordanoc.yachai.worker.ModelDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class WhiteboardStep(val prose: String, val equation: String, val highlight: String)

sealed interface WhiteboardUiState {
    object ModelNotDownloaded : WhiteboardUiState
    data class Downloading(val progress: ModelDownloadStatus) : WhiteboardUiState
    object Initializing : WhiteboardUiState
    object Idle : WhiteboardUiState
    object Loading : WhiteboardUiState
    data class Success(val steps: List<WhiteboardStep>) : WhiteboardUiState
    data class Error(val message: String) : WhiteboardUiState
}

class WhiteboardViewModel(
    application: Application,
    private val useGpuInitially: Boolean
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<WhiteboardUiState>(WhiteboardUiState.ModelNotDownloaded)
    val uiState = _uiState.asStateFlow()

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private var llmInference: LlmInference? = null
    private var isModelInitialized = false

    // GPU preference state
    private val _useGpu = MutableStateFlow(useGpuInitially)
    val useGpu = _useGpu.asStateFlow()

    // Define the model we want to use
    private val model = Models.GEMMA_3N_E2B_VISION

    init {
        checkModelStatus()
    }

    private fun checkModelStatus() {
        val context = getApplication<Application>().applicationContext
        when {
            model.isDownloaded(context) -> {
                Log.d(TAG, "Model already downloaded, initializing...")
                initializeModel()
            }
            else -> {
                Log.d(TAG, "Model not downloaded")
                _uiState.value = WhiteboardUiState.ModelNotDownloaded
            }
        }
    }

    fun downloadModel() {
        val context = getApplication<Application>().applicationContext
        val workManager = WorkManager.getInstance(context)
        
        Log.d(TAG, "Starting model download...")
        _uiState.value = WhiteboardUiState.Downloading(
            ModelDownloadStatus(status = ModelDownloadStatusType.IN_PROGRESS)
        )

        // Create input data for worker
        val inputData = Data.Builder()
            .putString(ModelDownloadWorker.KEY_MODEL_NAME, model.name)
            .putString(ModelDownloadWorker.KEY_MODEL_URL, model.url)
            .putString(ModelDownloadWorker.KEY_MODEL_DOWNLOAD_FILE_NAME, model.downloadFileName)
            .putString(ModelDownloadWorker.KEY_AUTH_TOKEN, "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")
            .build()

        // Create download work request
        val downloadWorkRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(inputData)
            .addTag("MODEL_DOWNLOAD:${model.name}")
            .build()

        // Start download
        workManager.enqueueUniqueWork(
            model.name,
            ExistingWorkPolicy.REPLACE,
            downloadWorkRequest
        )

        // Observe progress
        observeDownloadProgress(downloadWorkRequest.id)
    }

    private fun observeDownloadProgress(workerId: java.util.UUID) {
        val context = getApplication<Application>().applicationContext
        val workManager = WorkManager.getInstance(context)
        
        workManager.getWorkInfoByIdLiveData(workerId).observeForever { workInfo ->
            when (workInfo?.state) {
                WorkInfo.State.RUNNING -> {
                    val receivedBytes = workInfo.progress.getLong(ModelDownloadWorker.KEY_RECEIVED_BYTES, 0L)
                    val downloadRate = workInfo.progress.getLong(ModelDownloadWorker.KEY_DOWNLOAD_RATE, 0L)
                    val remainingMs = workInfo.progress.getLong(ModelDownloadWorker.KEY_REMAINING_MS, 0L)

                    _uiState.value = WhiteboardUiState.Downloading(
                        ModelDownloadStatus(
                            status = ModelDownloadStatusType.IN_PROGRESS,
                            totalBytes = model.sizeInBytes,
                            receivedBytes = receivedBytes,
                            bytesPerSecond = downloadRate,
                            remainingMs = remainingMs
                        )
                    )
                }

                WorkInfo.State.SUCCEEDED -> {
                    Log.d(TAG, "Model download completed, initializing...")
                    initializeModel()
                }

                WorkInfo.State.FAILED -> {
                    val errorMessage = workInfo.outputData.getString(ModelDownloadWorker.KEY_ERROR_MESSAGE) ?: "Download failed"
                    Log.e(TAG, "Model download failed: $errorMessage")
                    _uiState.value = WhiteboardUiState.Error("Download failed: $errorMessage")
                }

                WorkInfo.State.CANCELLED -> {
                    Log.d(TAG, "Model download cancelled")
                    _uiState.value = WhiteboardUiState.ModelNotDownloaded
                }

                else -> {}
            }
        }
    }

    private fun initializeModel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = WhiteboardUiState.Initializing
                val context = getApplication<Application>().applicationContext
                val modelPath = model.getLocalPath(context)
                
                Log.d(TAG, "Initializing model from: $modelPath")
                
                // Check if model file exists
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    Log.e(TAG, "Model file does not exist at: $modelPath")
                    _uiState.value = WhiteboardUiState.Error("Model file not found at: $modelPath")
                    return@launch
                }
                
                Log.d(TAG, "Model file exists, size: ${modelFile.length()} bytes")

                // Create LLM Inference options
                val backend = if (_useGpu.value) LlmInference.Backend.GPU else LlmInference.Backend.CPU
                Log.d(TAG, "Using backend: $backend")
                
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(512)
                    .setMaxNumImages(1)
                    .setPreferredBackend(backend)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                isModelInitialized = true
                
                Log.d(TAG, "Model initialized successfully")
                _uiState.value = WhiteboardUiState.Idle

            } catch (e: Exception) {
                Log.e(TAG, "Model initialization error: ${e.message}", e)
                _uiState.value = WhiteboardUiState.Error("Model initialization failed: ${e.localizedMessage}")
            }
        }
    }

    fun runInference(imageUri: Uri) {
        if (!isModelInitialized || llmInference == null) {
            _uiState.value = WhiteboardUiState.Error("Model is not initialized yet. Please wait and try again.")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = WhiteboardUiState.Loading
            
            try {
                val context = getApplication<Application>().applicationContext
                val bitmap = uriToBitmap(context, imageUri)
                
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(40)
                    .setTopP(0.95f)
                    .setTemperature(0.8f)
                    .setGraphOptions(
                        GraphOptions.builder()
                            .setEnableVisionModality(true)
                            .build()
                    )
                    .build()

                llmInference!!.use { inference ->
                    LlmInferenceSession.createFromOptions(inference, sessionOptions).use { session ->
                        val prompt = """
You are a friendly and encouraging math tutor. Given the following math equation, provide a solution as a JSON array of 'steps'.
Each step object must contain:
1. "prose": A short, simple text explanation of this step.
2. "equation": The mathematical state of the equation at the end of this step.
3. "highlight": The part of the equation that was changed in this step.

Example format:
[
  {
    "prose": "First, let's subtract 5 from both sides to isolate the term with x.",
    "equation": "2x/3 = 5",
    "highlight": "- 5"
  },
  {
    "prose": "Great! Now, let's multiply both sides by 3 to get rid of the fraction.",
    "equation": "2x = 15",
    "highlight": "* 3"
  }
]

Provide only the JSON array in your response.
Here is the math problem:
""".trimIndent()

                        val mpImage = BitmapImageBuilder(bitmap).build()
                        session.addQueryChunk(prompt)
                        session.addImage(mpImage)

                        val result = session.generateResponse()
                        processResponse(result)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed: ${e.stackTraceToString()}")
                _uiState.value = WhiteboardUiState.Error(e.localizedMessage ?: "Unknown error during inference")
            }
        }
    }

    private fun processResponse(response: String) {
        try {
            Log.d(TAG, "Received response: $response")
            // Find the start and end of the JSON array
            val jsonStartIndex = response.indexOf("[")
            val jsonEndIndex = response.lastIndexOf("]")

            if (jsonStartIndex != -1 && jsonEndIndex != -1) {
                val jsonString = response.substring(jsonStartIndex, jsonEndIndex + 1)
                try {
                    val steps = json.decodeFromString<List<WhiteboardStep>>(jsonString)
                    _uiState.value = WhiteboardUiState.Success(steps)
                } catch (e: Exception) {
                    Log.e(TAG, "JSON parsing failed: ${e.message}")
                    _uiState.value = WhiteboardUiState.Error(
                        "Failed to parse the explanation. The response was:\n$response"
                    )
                }
            } else {
                _uiState.value = WhiteboardUiState.Error(
                    "Could not find a valid JSON array in the AI's response."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Response processing failed: ${e.message}", e)
            _uiState.value = WhiteboardUiState.Error("Failed to process response: ${e.localizedMessage}")
        }
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    override fun onCleared() {
        super.onCleared()
        llmInference?.close()
    }

    // Utility functions for UI display
    fun formatBytes(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
