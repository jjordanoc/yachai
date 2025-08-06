package com.jjordanoc.yachai.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.jjordanoc.yachai.R
import com.jjordanoc.yachai.llm.data.Model
import com.jjordanoc.yachai.llm.data.ModelDownloadStatus
import com.jjordanoc.yachai.llm.data.ModelDownloadStatusType
import com.jjordanoc.yachai.utils.TAG
import com.jjordanoc.yachai.worker.ModelDownloadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(val progress: ModelDownloadStatus) : DownloadState
    object Success : DownloadState
    data class Error(val message: String) : DownloadState
}

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    private val workManager = WorkManager.getInstance(application)

    fun downloadModel(model: Model) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(
                ModelDownloadStatus(
                    ModelDownloadStatusType.IN_PROGRESS)
            )

            val inputData = Data.Builder()
                .putString(ModelDownloadWorker.KEY_MODEL_NAME, model.name)
                .putString(ModelDownloadWorker.KEY_MODEL_URL, model.url)
                .putString(ModelDownloadWorker.KEY_MODEL_DOWNLOAD_FILE_NAME, model.downloadFileName)
                .putLong(ModelDownloadWorker.KEY_MODEL_TOTAL_BYTES, model.sizeInBytes)
                .putString(ModelDownloadWorker.KEY_AUTH_TOKEN, "YOUR_HF_TOKEN")
                .build()

            val downloadWorkRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(inputData)
                .addTag("MODEL_DOWNLOAD:${model.name}")
                .build()

            workManager.enqueueUniqueWork(
                model.name,
                ExistingWorkPolicy.REPLACE,
                downloadWorkRequest
            )

            observeDownloadProgress(downloadWorkRequest.id, model.sizeInBytes)
        }
    }

    private fun observeDownloadProgress(workerId: java.util.UUID, totalBytes: Long) {
        workManager.getWorkInfoByIdLiveData(workerId).observeForever { workInfo ->
            when (workInfo?.state) {
                WorkInfo.State.RUNNING -> {
                    val receivedBytes = workInfo.progress.getLong(ModelDownloadWorker.KEY_RECEIVED_BYTES, 0L)
                    val downloadRate = workInfo.progress.getLong(ModelDownloadWorker.KEY_DOWNLOAD_RATE, 0L)
                    val remainingMs = workInfo.progress.getLong(ModelDownloadWorker.KEY_REMAINING_MS, 0L)

                    _downloadState.value = DownloadState.Downloading(
                        ModelDownloadStatus(
                            status = ModelDownloadStatusType.IN_PROGRESS,
                            totalBytes = totalBytes,
                            receivedBytes = receivedBytes,
                            bytesPerSecond = downloadRate,
                            remainingMs = remainingMs
                        )
                    )
                }
                WorkInfo.State.SUCCEEDED -> {
                    _downloadState.value = DownloadState.Success
                }
                WorkInfo.State.FAILED -> {
                    val errorMessage = workInfo.outputData.getString(ModelDownloadWorker.KEY_ERROR_MESSAGE) ?: "Download failed"
                    _downloadState.value = DownloadState.Error(errorMessage)
                }
                WorkInfo.State.CANCELLED -> {
                    _downloadState.value = DownloadState.Error("Download cancelled")
                }
                else -> {}
            }
        }
    }
}
