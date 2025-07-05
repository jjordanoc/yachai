package com.jjordanoc.yachai.data

enum class ModelDownloadStatusType {
    NOT_DOWNLOADED,      // Model hasn't been downloaded yet
    PARTIALLY_DOWNLOADED, // Download was interrupted, can be resumed
    IN_PROGRESS,         // Currently downloading
    UNZIPPING,          // Extracting zip file after download
    SUCCEEDED,          // Download completed successfully
    FAILED              // Download failed with error
}

data class ModelDownloadStatus(
    val status: ModelDownloadStatusType,
    val totalBytes: Long = 0,
    val receivedBytes: Long = 0,
    val errorMessage: String = "",
    val bytesPerSecond: Long = 0,    // Download speed
    val remainingMs: Long = 0        // Estimated time remaining
)

data class Model(
    val name: String,
    val url: String,
    val downloadFileName: String,
    val sizeInBytes: Long,
    val version: String = "_",
    val isZip: Boolean = false,
    val unzipDir: String = "",
    // Runtime fields
    var normalizedName: String = "",
    var totalBytes: Long = 0L,
    var accessToken: String? = null
)

// Extension function to get model file path
fun Model.getLocalPath(context: android.content.Context): String {
    return "${context.getExternalFilesDir(null)?.absolutePath}/$downloadFileName"
}

// Extension function to check if model is downloaded
fun Model.isDownloaded(context: android.content.Context): Boolean {
    val file = java.io.File(getLocalPath(context))
    if (!file.exists()) return false
    // The defined size is an approximation, so check if the file size is reasonably close.
    if (sizeInBytes > 0) {
        return file.length() >= sizeInBytes * 0.5
    }
    return file.length() > 0
} 