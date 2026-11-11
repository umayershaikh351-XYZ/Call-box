package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingRepository(
    private val context: Context,
    private val recordingDao: RecordingDao
) {
    val allRecordings: Flow<List<Recording>> = recordingDao.getAll()
    val recentRecordings: Flow<List<Recording>> = recordingDao.getRecent(5)
    val totalCount: Flow<Int> = recordingDao.getCount()
    val totalSizeBytes: Flow<Long> = recordingDao.getTotalSizeBytes()
    val totalDurationMs: Flow<Long> = recordingDao.getTotalDurationMs()

    fun searchRecordings(query: String): Flow<List<Recording>> = recordingDao.search(query)

    fun filterByApp(appPackage: String): Flow<List<Recording>> = recordingDao.filterByApp(appPackage)

    fun getRecordingsDir(): File {
        val dir = File(context.getExternalFilesDir(null), "Recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun generateNewOutputFile(appLabel: String): File {
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanLabel = appLabel.replace(Regex("[^a-zA-Z0-9]"), "")
        val fileName = "${cleanLabel}_$timestampStr.m4a"
        return File(getRecordingsDir(), fileName)
    }

    suspend fun saveRecording(
        file: File,
        appPackage: String,
        appLabel: String,
        durationMs: Long
    ): Recording = withContext(Dispatchers.IO) {
        val sizeBytes = if (file.exists()) file.length() else 0L
        val recording = Recording(
            fileName = file.name,
            filePath = file.absolutePath,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            appPackage = appPackage,
            appLabel = appLabel,
            timestamp = System.currentTimeMillis(),
            isStarred = false
        )
        val id = recordingDao.insert(recording)
        recording.copy(id = id)
    }

    suspend fun toggleStar(recording: Recording) = withContext(Dispatchers.IO) {
        recordingDao.update(recording.copy(isStarred = !recording.isStarred))
    }

    suspend fun renameRecording(recording: Recording, newBaseName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanName = newBaseName.trim().replace(Regex("[/\\\\?%*:|\"<>]"), "_")
            val newFileName = if (cleanName.endsWith(".m4a", ignoreCase = true)) cleanName else "$cleanName.m4a"
            val currentFile = File(recording.filePath)
            val newFile = File(currentFile.parentFile, newFileName)

            if (currentFile.exists() && currentFile.renameTo(newFile)) {
                val updated = recording.copy(
                    fileName = newFileName,
                    filePath = newFile.absolutePath
                )
                recordingDao.update(updated)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteRecording(recording: Recording) = withContext(Dispatchers.IO) {
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
            recordingDao.delete(recording)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteAllRecordings() = withContext(Dispatchers.IO) {
        try {
            val dir = getRecordingsDir()
            dir.listFiles()?.forEach { it.delete() }
            recordingDao.deleteAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareRecording(recording: Recording) {
        val file = File(recording.filePath)
        if (!file.exists()) return

        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            android.net.Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, recording.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Call Recording").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
