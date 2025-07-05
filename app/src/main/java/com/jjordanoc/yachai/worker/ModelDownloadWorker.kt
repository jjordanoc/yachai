package com.jjordanoc.yachai.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.jjordanoc.yachai.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MODEL_NAME = "model_name"
        const val KEY_MODEL_URL = "model_url"
        const val KEY_MODEL_DOWNLOAD_FILE_NAME = "model_download_file_name"
        const val KEY_MODEL_TOTAL_BYTES = "model_total_bytes"
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_RECEIVED_BYTES = "received_bytes"
        const val KEY_DOWNLOAD_RATE = "download_rate"
        const val KEY_REMAINING_MS = "remaining_ms"
        const val KEY_ERROR_MESSAGE = "error_message"
        
        const val CHANNEL_ID = "model_download_channel"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Model"
        return createForegroundInfo(0, modelName)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val modelUrl = inputData.getString(KEY_MODEL_URL) ?: return@withContext Result.failure()
            val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Model"
            val fileName = inputData.getString(KEY_MODEL_DOWNLOAD_FILE_NAME) ?: return@withContext Result.failure()
            val totalBytes = inputData.getLong(KEY_MODEL_TOTAL_BYTES, 0L)
            val authToken = inputData.getString(KEY_AUTH_TOKEN)

            // Create notification channel
            createNotificationChannel()
            
            // Prepare output file
            val outputFile = File(context.getExternalFilesDir(null), fileName)
            val outputDir = outputFile.parentFile
            if (outputDir?.exists() != true) {
                outputDir?.mkdirs()
            }

            // Check for partial download and resume if possible
            val existingBytes = if (outputFile.exists()) outputFile.length() else 0L

            // Open connection with resume support
            val url = URL(modelUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            // Add authentication if token is provided
            if (!authToken.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $authToken")
            }
            
            // Add resume support if partial download exists
            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=${existingBytes}-")
            }
            
            // Enable redirects and connect
            connection.instanceFollowRedirects = true
            connection.connect()
            
            // Check response code
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != 206 /* HttpURLConnection.HTTP_PARTIAL_CONTENT */) {
                throw Exception("HTTP error: $responseCode ${connection.responseMessage}")
            }

            // Download with progress reporting
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile, existingBytes > 0)

            val buffer = ByteArray(8192)
            var downloadedBytes = existingBytes
            var lastProgressTime = 0L
            val bytesReadBuffer = mutableListOf<Long>()
            val timeBuffer = mutableListOf<Long>()

            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                // Report progress every 500ms
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastProgressTime > 500) {
                    // Calculate download rate
                    val deltaBytes = downloadedBytes - (bytesReadBuffer.lastOrNull() ?: 0L)
                    val deltaTime = currentTime - (timeBuffer.lastOrNull() ?: currentTime)

                    if (bytesReadBuffer.size >= 5) {
                        bytesReadBuffer.removeAt(0)
                        timeBuffer.removeAt(0)
                    }
                    bytesReadBuffer.add(downloadedBytes)
                    timeBuffer.add(currentTime)

                    val bytesPerMs = if (deltaTime > 0) deltaBytes.toFloat() / deltaTime else 0f
                    val remainingMs = if (bytesPerMs > 0) ((totalBytes - downloadedBytes) / bytesPerMs).toLong() else 0L

                    // Report progress
                    setProgress(Data.Builder()
                        .putLong(KEY_RECEIVED_BYTES, downloadedBytes)
                        .putLong(KEY_DOWNLOAD_RATE, (bytesPerMs * 1000).toLong())
                        .putLong(KEY_REMAINING_MS, remainingMs)
                        .build())

                    // Update foreground notification
                    val progress = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else 0
                    setForeground(createForegroundInfo(progress, modelName))

                    lastProgressTime = currentTime
                }
            }

            outputStream.close()
            inputStream.close()

            Result.success()
        } catch (e: Exception) {
            Result.failure(Data.Builder()
                .putString(KEY_ERROR_MESSAGE, e.message ?: "Unknown error")
                .build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for AI model downloads"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(progress: Int, modelName: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading \"$modelName\"")
            .setContentText("Progress: $progress%")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // You may want to use a download icon
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
} 