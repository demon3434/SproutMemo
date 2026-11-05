package com.sproutmemo.android.ui.note

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.*
import com.sproutmemo.android.ui.components.LocalMediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class NoteEditViewModel(application: Application) : NoteFormViewModel(application, autoSelectDefaults = false) {

    private val _submitState = MutableStateFlow<CreateNoteState>(CreateNoteState.Idle)
    override val submitState: StateFlow<CreateNoteState> = _submitState

    val noteId = MutableStateFlow("")
    val syncBaseTime = MutableStateFlow("")
    val deleteAttachmentIds = mutableListOf<String>()

    private val gson = Gson()

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            try {
                val res = apiService.getNoteDetail(noteId)
                if (res.isSuccessful && res.body()?.code == 200) {
                    val note = res.body()?.data ?: return@launch

                    selectedStageId.value = note.stage_id
                    selectedSemesterId.value = note.semester_id

                    // Fetch subjects for this semester
                    val subRes = apiService.getSemesterSubjects(semesterId = note.semester_id)
                    if (subRes.isSuccessful && subRes.body()?.code == 200) {
                        subjects.value = subRes.body()?.data?.items ?: emptyList()
                    }
                    selectedSubjectId.value = note.subject_id

                    // Fetch books for this semester/subject
                    val bookRes = apiService.getQuestionBooks(semesterId = note.semester_id, subjectId = note.subject_id)
                    if (bookRes.isSuccessful && bookRes.body()?.code == 200) {
                        books.value = bookRes.body()?.data?.items ?: emptyList()
                    }

                    selectedUnitCode.value = note.unit_code ?: ""
                    sourceType.value = note.source_type
                    selectedQuestionSourceId.value = note.question_source_id ?: ""
                    paperCategory.value = note.paper_category ?: "MIDTERM"
                    sourcePaperName.value = note.source_paper_name ?: ""
                    selectedUnitCode.value = note.unit_code ?: ""
                    pageNumber.value = note.page_number ?: ""
                    difficultyStars.value = note.difficulty_stars
                    masteryLevel.value = note.mastery_level
                    inReviewPool.value = note.in_review_pool
                    auxiliaryText.value = note.auxiliary_text ?: ""

                    // Map remote attachments
                    val items = note.attachments.sortedBy { it.display_order }.map { att ->
                        val fullUrl = ApiClient.getFullAttachmentUrl(getApplication(), att.storage_path)
                        LocalMediaItem(
                            uri = Uri.parse(fullUrl),
                            type = when (att.file_type?.uppercase()) {
                                "VIDEO" -> "video"
                                "AUDIO" -> "audio"
                                else -> "image"
                            },
                            name = att.title ?: "",
                            id = att.id,
                            size = att.file_size
                        )
                    }
                    localMediaItems.value = items
                    deleteAttachmentIds.clear()
                    syncBaseTime.value = note.updated_at ?: ""
                    this@NoteEditViewModel.noteId.value = note.id
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun removeMediaItem(index: Int) {
        val current = localMediaItems.value.toMutableList()
        if (index in current.indices) {
            val removed = current.removeAt(index)
            localMediaItems.value = current
            removed.id?.let { id ->
                deleteAttachmentIds.add(id)
            }
        }
    }

    override fun submitNote() {
        if (_submitState.value is CreateNoteState.Submitting) return

        if (selectedStageId.value.isBlank()) {
            _submitState.value = CreateNoteState.Error("请选择学习阶段")
            return
        }
        if (selectedSemesterId.value.isBlank()) {
            _submitState.value = CreateNoteState.Error("请选择学期")
            return
        }
        if (selectedSubjectId.value.isBlank()) {
            _submitState.value = CreateNoteState.Error("请选择学科")
            return
        }

        if (sourceType.value == "BOOK") {
            if (selectedQuestionSourceId.value.isBlank()) {
                _submitState.value = CreateNoteState.Error("请选择关联的已存习题册")
                return
            }
            if (selectedUnitCode.value.isBlank()) {
                _submitState.value = CreateNoteState.Error("请选择章节单元")
                return
            }
            if (pageNumber.value.isBlank()) {
                _submitState.value = CreateNoteState.Error("请输入页码")
                return
            }
            if (!pageNumber.value.any { it.isDigit() }) {
                _submitState.value = CreateNoteState.Error("页码格式不正确，必须包含数字数值")
                return
            }
        } else {
            if (paperCategory.value.isBlank()) {
                _submitState.value = CreateNoteState.Error("请选择试卷分类")
                return
            }
            if (selectedUnitCode.value.isBlank()) {
                _submitState.value = CreateNoteState.Error("请选择章节单元")
                return
            }
            if (sourcePaperName.value.isBlank()) {
                _submitState.value = CreateNoteState.Error("请输入自定义来源/试卷名称")
                return
            }
        }

        viewModelScope.launch {
            _submitState.value = CreateNoteState.Submitting
            try {
                val res = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val fields = mutableMapOf<String, okhttp3.RequestBody>()
                    fun addField(key: String, value: String) {
                        fields[key] = value.toRequestBody("text/plain".toMediaTypeOrNull())
                    }

                    addField("stage_id", selectedStageId.value)
                    addField("semester_id", selectedSemesterId.value)
                    addField("subject_id", selectedSubjectId.value)
                    addField("source_type", sourceType.value)
                    addField("difficulty_stars", difficultyStars.value.toString())
                    addField("mastery_level", masteryLevel.value)
                    addField("exclude_from_review", (!inReviewPool.value).toString())
                    if (auxiliaryText.value.isNotBlank()) addField("auxiliary_text", auxiliaryText.value)

                    if (sourceType.value == "BOOK") {
                        addField("question_source_id", selectedQuestionSourceId.value)
                        addField("unit_code", selectedUnitCode.value)
                        addField("page_number", pageNumber.value.trim())
                    } else {
                        addField("paper_category", paperCategory.value)
                        addField("unit_code", selectedUnitCode.value)
                        addField("source_paper_name", sourcePaperName.value.trim())
                    }

                    addField("sync_base_time", syncBaseTime.value)
                    addField("force", "false")

                    val parts = mutableListOf<MultipartBody.Part>()

                    // Add deleted attachments
                    deleteAttachmentIds.forEach { id ->
                        parts.add(MultipartBody.Part.createFormData("delete_attachment_ids", id))
                    }

                    // Add existing remaining attachments metadata (including any rotation degrees)
                    val remainingMeta = mutableListOf<Map<String, Any>>()
                    var sortOrderCounter = 0
                    localMediaItems.value.forEach { item ->
                        if (item.id != null) {
                            remainingMeta.add(mapOf(
                                "id" to item.id,
                                "title" to item.name,
                                "sort_order" to sortOrderCounter++,
                                "rotation" to item.rotation.toInt()
                            ))
                        }
                    }
                    val attachmentMetaJson = gson.toJson(remainingMeta)
                    addField("attachment_metadata", attachmentMetaJson)

                    // Upload new attachments (where id is null)
                    val itemsToUpload = localMediaItems.value.filter { it.id == null }
                    itemsToUpload.forEach { item ->
                        val compressedFile: File? = when (item.type) {
                            "image" -> {
                                val cUri = item.compressedUri
                                if (cUri != null && cUri.scheme == "file" && item.rotation % 360f == 0f) {
                                    val file = File(cUri.path ?: "")
                                    if (file.exists()) file else compressor.compressImage(item.uri, item.rotation)
                                } else {
                                    compressor.compressImage(item.uri, item.rotation)
                                }
                            }
                            "video" -> {
                                val cUri = item.compressedUri
                                if (cUri != null && cUri.scheme == "file") {
                                    val file = File(cUri.path ?: "")
                                    if (file.exists()) file else compressor.compressVideo(item.uri, selectedVideoResolution.value)
                                } else {
                                    compressor.compressVideo(item.uri, selectedVideoResolution.value)
                                }
                            }
                            else -> {
                                val cUri = item.compressedUri
                                if (cUri != null && cUri.scheme == "file") {
                                    val file = File(cUri.path ?: "")
                                    if (file.exists()) file else {
                                        val cacheDir = getApplication<Application>().cacheDir
                                        val tempFile = File(cacheDir, "AUD_${System.currentTimeMillis()}.m4a")
                                        getApplication<Application>().contentResolver.openInputStream(item.uri)?.use { input ->
                                            tempFile.outputStream().use { output -> input.copyTo(output) }
                                        }
                                        tempFile
                                    }
                                } else {
                                    val cacheDir = getApplication<Application>().cacheDir
                                    val tempFile = File(cacheDir, "AUD_${System.currentTimeMillis()}.m4a")
                                    getApplication<Application>().contentResolver.openInputStream(item.uri)?.use { input ->
                                        tempFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    tempFile
                                }
                            }
                        }

                        if (compressedFile != null && compressedFile.exists()) {
                            val (partName, mimeType) = when (item.type) {
                                "video" -> Pair("videos", "video/mp4")
                                "audio" -> Pair("audios", "audio/aac")
                                else -> Pair("images", "image/jpeg")
                            }
                            val reqBody = compressedFile.asRequestBody(mimeType.toMediaTypeOrNull())
                            parts.add(MultipartBody.Part.createFormData(partName, compressedFile.name, reqBody))
                        }
                    }

                    itemsToUpload.filter { it.type == "image" }.forEach { item ->
                        parts.add(MultipartBody.Part.createFormData("image_titles", item.name))
                    }
                    itemsToUpload.filter { it.type == "audio" }.forEach { item ->
                        parts.add(MultipartBody.Part.createFormData("audio_titles", item.name))
                    }
                    itemsToUpload.filter { it.type == "video" }.forEach { item ->
                        parts.add(MultipartBody.Part.createFormData("video_titles", item.name))
                    }

                    apiService.updateNote(noteId.value, fields, parts)
                }

                if (res.isSuccessful && res.body()?.code == 200) {
                    _submitState.value = CreateNoteState.Success
                } else {
                    val rawError = res.errorBody()?.string()
                    val errorMsg = try {
                        if (!rawError.isNullOrEmpty()) {
                            val json = gson.fromJson(rawError, Map::class.java)
                            (json["msg"] as? String) ?: (json["detail"] as? String) ?: "保存失败 (${res.code()})"
                        } else {
                            res.body()?.msg ?: "保存失败 (${res.code()})"
                        }
                    } catch (e: Exception) {
                        "保存失败 (${res.code()})"
                    }
                    _submitState.value = CreateNoteState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _submitState.value = CreateNoteState.Error("网络请求失败: ${e.localizedMessage}")
            }
        }
    }
}
