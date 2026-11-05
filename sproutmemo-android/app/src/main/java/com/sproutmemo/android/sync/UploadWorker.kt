package com.sproutmemo.android.sync

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sproutmemo.android.data.local.DraftDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            DraftDatabase::class.java,
            "sproutmemo_drafts.db"
        ).build()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dao = db.draftDao()
        val pendingNotes = dao.getPendingNotes()

        if (pendingNotes.isEmpty()) {
            return@withContext Result.success()
        }

        Log.i("UploadWorker", "Found ${pendingNotes.size} pending drafts to sync...")

        for (note in pendingNotes) {
            val attachments = dao.getAttachmentsForNote(note.id)
            try {
                // Here we would make the Retrofit network call:
                // val response = apiService.uploadNote(
                //     subject = RequestBody.create(MediaType.parse("text/plain"), note.subject),
                //     ...
                //     images = multipartFiles
                // )
                
                // Simulating successful network upload:
                Log.d("UploadWorker", "Successfully uploaded note ${note.id} to sproutmemo API")
                
                // Delete successfully uploaded note from local DB
                dao.deleteNote(note)
                dao.deleteAttachmentsForNote(note.id)
            } catch (e: Exception) {
                Log.e("UploadWorker", "Failed to sync note ${note.id}: ${e.message}")
                // If it fails, retry later when network constraints are satisfied again
                return@withContext Result.retry()
            }
        }

        Result.success()
    }
}
