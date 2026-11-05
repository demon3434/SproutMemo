package com.sproutmemo.android.ui.note

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class NoteCreateViewModel(application: Application) : NoteFormViewModel(application) {

    private val _submitState = MutableStateFlow<CreateNoteState>(CreateNoteState.Idle)
    override val submitState: StateFlow<CreateNoteState> = _submitState

    private val gson = com.google.gson.Gson()

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

                    val parts = mutableListOf<MultipartBody.Part>()
                    localMediaItems.value.forEach { item ->
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

                    localMediaItems.value.filter { it.type == "image" }.forEach { item ->
                        parts.add(MultipartBody.Part.createFormData("image_titles", item.name))
                    }
                    localMediaItems.value.filter { it.type == "audio" }.forEach { item ->
                        parts.add(MultipartBody.Part.createFormData("audio_titles", item.name))
                    }
                    localMediaItems.value.filter { it.type == "video" }.forEach { item ->
                        parts.add(MultipartBody.Part.createFormData("video_titles", item.name))
                    }

                    apiService.createNote(fields, parts)
                }

                if (res.isSuccessful && res.body()?.code == 200) {
                    _submitState.value = CreateNoteState.Success
                } else {
                    val rawError = res.errorBody()?.string()
                    val errorMsg = try {
                        if (!rawError.isNullOrEmpty()) {
                            val json = gson.fromJson(rawError, Map::class.java)
                            (json["msg"] as? String) ?: (json["detail"] as? String) ?: "录入失败 (${res.code()})"
                        } else {
                            res.body()?.msg ?: "录入失败 (${res.code()})"
                        }
                    } catch (e: Exception) {
                        "录入失败 (${res.code()})"
                    }
                    _submitState.value = CreateNoteState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _submitState.value = CreateNoteState.Error("网络请求失败: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _submitState.value = CreateNoteState.Idle
        auxiliaryText.value = ""
        localMediaItems.value = emptyList()
    }
}
