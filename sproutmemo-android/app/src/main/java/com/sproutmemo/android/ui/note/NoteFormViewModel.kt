package com.sproutmemo.android.ui.note

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.*
import com.sproutmemo.android.media.MediaCompressor
import com.sproutmemo.android.ui.components.LocalMediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class CreateNoteState {
    object Idle : CreateNoteState()
    object Submitting : CreateNoteState()
    object Success : CreateNoteState()
    data class Error(val message: String) : CreateNoteState()
}

abstract class NoteFormViewModel(
    application: Application,
    protected val autoSelectDefaults: Boolean = true
) : AndroidViewModel(application) {

    abstract val submitState: StateFlow<CreateNoteState>
    abstract fun submitNote()

    protected val apiService = ApiClient.getService(application)
    protected val compressor = MediaCompressor(application)
    protected val tokenManager = ApiClient.getTokenManager(application)

    val stages = MutableStateFlow<List<StageDto>>(emptyList())
    val semesters = MutableStateFlow<List<SemesterDto>>(emptyList())
    val subjects = MutableStateFlow<List<SemesterSubjectMappingDto>>(emptyList())
    val books = MutableStateFlow<List<QuestionBookDto>>(emptyList())
    val units = MutableStateFlow<List<UnitDto>>(emptyList())
    val filteredUnits = MutableStateFlow<List<UnitDto>>(emptyList())
    val paperCategories = MutableStateFlow<List<PaperCategoryDto>>(emptyList())

    val selectedStageId = MutableStateFlow("")
    val selectedSemesterId = MutableStateFlow("")
    val selectedSubjectId = MutableStateFlow("")
    val sourceType = MutableStateFlow("BOOK") // BOOK, PAPER
    val selectedQuestionSourceId = MutableStateFlow("")
    val paperCategory = MutableStateFlow("MIDTERM")
    val sourcePaperName = MutableStateFlow("")
    val selectedUnitCode = MutableStateFlow("")
    val pageNumber = MutableStateFlow("")
    val difficultyStars = MutableStateFlow(3)
    val masteryLevel = MutableStateFlow("CLUELESS")
    val inReviewPool = MutableStateFlow(true)
    val auxiliaryText = MutableStateFlow("")
    val localMediaItems = MutableStateFlow<List<LocalMediaItem>>(emptyList())
    val selectedVideoResolution = MutableStateFlow("720p")

    val paperSuggestions = MutableStateFlow<List<QuestionBookDto>>(emptyList())
    val showSuggestions = MutableStateFlow(false)

    init {
        loadMetadata()
        loadVideoQualitySetting()
        viewModelScope.launch {
            combine(units, sourceType, paperCategory) { _, _, _ -> }.collect {
                updateFilteredUnits()
            }
        }
    }

    private fun loadVideoQualitySetting() {
        viewModelScope.launch {
            tokenManager.videoQualityFlow.collect { quality ->
                selectedVideoResolution.value = quality
            }
        }
    }

    fun updateMediaName(index: Int, name: String) {
        val current = localMediaItems.value.toMutableList()
        if (index in current.indices) {
            val old = current[index]
            current[index] = old.copy(name = name)
            localMediaItems.value = current
        }
    }

    fun fetchPaperSuggestions(query: String) {
        if (selectedSemesterId.value.isBlank() || selectedSubjectId.value.isBlank()) return
        viewModelScope.launch {
            try {
                val studentId = tokenManager.getUserIdSync() ?: ""
                val res = apiService.getQuestionBooks(
                    studentId = studentId,
                    semesterId = selectedSemesterId.value,
                    subjectId = selectedSubjectId.value,
                    sourceType = "PAPER",
                    categoryCode = paperCategory.value,
                    unitCode = selectedUnitCode.value.ifBlank { null },
                    nameQuery = query.trim().ifBlank { null }
                )
                if (res.isSuccessful && res.body()?.code == 200) {
                    paperSuggestions.value = res.body()?.data?.items ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected open fun loadMetadata() {
        viewModelScope.launch {
            try {
                val stageRes = apiService.getStages()
                if (stageRes.isSuccessful && stageRes.body()?.code == 200) {
                    val list = stageRes.body()?.data?.items ?: emptyList()
                    stages.value = list
                    if (autoSelectDefaults && list.isNotEmpty()) selectedStageId.value = list[0].id
                }

                val semRes = apiService.getSemesters()
                if (semRes.isSuccessful && semRes.body()?.code == 200) {
                    val list = semRes.body()?.data?.items ?: emptyList()
                    semesters.value = list
                }

                val unitRes = apiService.getUnits()
                if (unitRes.isSuccessful && unitRes.body()?.code == 200) {
                    val list = unitRes.body()?.data?.items ?: emptyList()
                    units.value = list
                    if (autoSelectDefaults && list.isNotEmpty()) selectedUnitCode.value = list[0].id
                }

                val catsRes = apiService.getPaperCategories()
                if (catsRes.isSuccessful && catsRes.body()?.code == 200) {
                    val list = catsRes.body()?.data?.items ?: emptyList()
                    paperCategories.value = list
                    if (autoSelectDefaults && list.isNotEmpty()) paperCategory.value = list[0].code
                }

                if (!autoSelectDefaults) return@launch

                val userRes = apiService.getCurrentUser()
                var studentProfile: StudentProfileDto? = null
                if (userRes.isSuccessful && userRes.body()?.code == 200) {
                    val user = userRes.body()?.data
                    if (user != null) {
                        if (user.role == "STUDENT") {
                            studentProfile = user.student_profile ?: StudentProfileDto(
                                enrollment_year = user.enrollment_year ?: 2024,
                                semester_offset = user.semester_offset ?: 0
                            )
                        } else if (user.role == "PARENT" && !user.bound_students.isNullOrEmpty()) {
                            val firstChild = user.bound_students[0]
                            studentProfile = firstChild.student_profile ?: StudentProfileDto(
                                enrollment_year = firstChild.enrollment_year ?: 2024,
                                semester_offset = firstChild.semester_offset ?: 0
                            )
                        }
                    }
                }

                if (studentProfile != null && semesters.value.isNotEmpty()) {
                    val targetSemName = calculateCurrentSemester(
                        studentProfile.enrollment_year,
                        studentProfile.semester_offset
                    )
                    val matchedSem = semesters.value.find { it.name == targetSemName }
                    if (matchedSem != null) {
                        selectedStageId.value = matchedSem.stage_id
                        selectedSemesterId.value = matchedSem.id
                        onSemesterChanged(matchedSem.id)
                    } else {
                        val fallbackSem = semesters.value[0]
                        selectedStageId.value = fallbackSem.stage_id
                        selectedSemesterId.value = fallbackSem.id
                        onSemesterChanged(fallbackSem.id)
                    }
                } else if (semesters.value.isNotEmpty()) {
                    val fallbackSem = semesters.value[0]
                    selectedStageId.value = fallbackSem.stage_id
                    selectedSemesterId.value = fallbackSem.id
                    onSemesterChanged(fallbackSem.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateCurrentSemester(enrollmentYear: Int, semesterOffset: Int = 0): String {
        val now = java.util.Calendar.getInstance()
        val currentYear = now.get(java.util.Calendar.YEAR)
        val currentMonth = now.get(java.util.Calendar.MONTH) + 1

        var isFirstTerm = false
        var calculationYear = currentYear

        if (currentMonth >= 9) {
            isFirstTerm = true
        } else if (currentMonth == 1) {
            isFirstTerm = true
            calculationYear = currentYear - 1
        } else {
            isFirstTerm = false
            calculationYear = currentYear - 1
        }

        var termSequence = (calculationYear - enrollmentYear) * 2 + (if (isFirstTerm) 1 else 2)
        termSequence += semesterOffset

        if (termSequence < 1) termSequence = 1
        if (termSequence > 24) termSequence = 24

        val grades = mapOf(
            1 to "小一",
            2 to "小二",
            3 to "小三",
            4 to "小四",
            5 to "小五",
            6 to "小六",
            7 to "初一",
            8 to "初二",
            9 to "初三",
            10 to "高一",
            11 to "高二",
            12 to "高三"
        )

        val gradeIndex = (termSequence - 1) / 2 + 1
        val isUpper = termSequence % 2 == 1

        val gradeName = grades[gradeIndex] ?: "第${gradeIndex}学级"
        val termName = if (isUpper) "上" else "下"

        return "$gradeName$termName"
    }

    fun onStageChanged(stageId: String) {
        selectedStageId.value = stageId
        val filteredSems = semesters.value.filter { it.stage_id == stageId }
        if (filteredSems.isNotEmpty()) {
            selectedSemesterId.value = filteredSems[0].id
            onSemesterChanged(filteredSems[0].id)
        } else {
            selectedSemesterId.value = ""
            subjects.value = emptyList()
            selectedSubjectId.value = ""
            books.value = emptyList()
            selectedQuestionSourceId.value = ""
        }
    }

    fun onSemesterChanged(semesterId: String) {
        selectedSemesterId.value = semesterId
        if (semesterId.isBlank()) {
            subjects.value = emptyList()
            selectedSubjectId.value = ""
            books.value = emptyList()
            selectedQuestionSourceId.value = ""
            return
        }
        viewModelScope.launch {
            try {
                val subRes = apiService.getSemesterSubjects(semesterId = semesterId)
                if (subRes.isSuccessful && subRes.body()?.code == 200) {
                    val list = subRes.body()?.data?.items ?: emptyList()
                    subjects.value = list
                    if (list.isNotEmpty()) {
                        selectedSubjectId.value = list[0].subject_id
                        onSubjectChanged(list[0].subject_id)
                    } else {
                        selectedSubjectId.value = ""
                        books.value = emptyList()
                        selectedQuestionSourceId.value = ""
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onSubjectChanged(subjectId: String) {
        selectedSubjectId.value = subjectId
        if (subjectId.isBlank()) {
            books.value = emptyList()
            selectedQuestionSourceId.value = ""
            return
        }
        viewModelScope.launch {
            try {
                val bookRes = apiService.getQuestionBooks(semesterId = selectedSemesterId.value, subjectId = subjectId)
                if (bookRes.isSuccessful && bookRes.body()?.code == 200) {
                    val list = bookRes.body()?.data?.items ?: emptyList()
                    books.value = list
                    if (list.isNotEmpty()) {
                        selectedQuestionSourceId.value = list[0].id
                    } else {
                        selectedQuestionSourceId.value = ""
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val rotationCompressionJobs = mutableMapOf< Uri, kotlinx.coroutines.Job >()

    fun addMediaItem(uri: Uri, type: String) {
        val current = localMediaItems.value.toMutableList()
        if (current.any { it.uri == uri }) {
            return
        }
        if (current.size < 9) {
            val newItem = LocalMediaItem(uri = uri, type = type)
            current.add(newItem)
            localMediaItems.value = current

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val compressedFile: File? = when (type) {
                        "image" -> compressor.compressImage(uri, 0f)
                        "video" -> compressor.compressVideo(uri, selectedVideoResolution.value)
                        "audio" -> {
                            val cacheDir = getApplication<Application>().cacheDir
                            val tempFile = File(cacheDir, "AUD_${System.currentTimeMillis()}.m4a")
                            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                                tempFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            if (tempFile.exists() && tempFile.length() > 0) tempFile else null
                        }
                        else -> null
                    }
                    if (compressedFile != null && compressedFile.exists()) {
                        updateCompressedUriByTarget(uri, Uri.fromFile(compressedFile))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateCompressedUriByTarget(targetUri: Uri, compressedUri: Uri) {
        val current = localMediaItems.value.toMutableList()
        val index = current.indexOfFirst { it.uri == targetUri }
        if (index != -1) {
            val old = current[index]
            current[index] = old.copy(compressedUri = compressedUri)
            localMediaItems.value = current
        }
    }

    fun rotateMediaBy90(index: Int) {
        val current = localMediaItems.value
        if (index in current.indices) {
            val newDegrees = (current[index].rotation + 90f) % 360f
            updateMediaRotation(index, newDegrees)
        }
    }

    fun updateMediaRotation(index: Int, degrees: Float) {
        val current = localMediaItems.value.toMutableList()
        if (index in current.indices) {
            val old = current[index]
            val normalizedDegrees = ((degrees % 360f) + 360f) % 360f
            current[index] = old.copy(rotation = normalizedDegrees)
            localMediaItems.value = current

            if (old.type == "image" && old.id == null) {
                rotationCompressionJobs[old.uri]?.cancel()
                rotationCompressionJobs[old.uri] = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val compressedFile = compressor.compressImage(old.uri, normalizedDegrees)
                    if (compressedFile != null && compressedFile.exists()) {
                        updateCompressedUriByTarget(old.uri, Uri.fromFile(compressedFile))
                    }
                }
            }
        }
    }

    open fun removeMediaItem(index: Int) {
        val current = localMediaItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            localMediaItems.value = current
        }
    }

    fun moveMediaItemWithinType(type: String, itemIndexInGroup: Int, direction: Int) {
        val current = localMediaItems.value.toMutableList()
        val absoluteIndices = current.indices.filter { current[it].type == type }
        val targetGroupIndex = itemIndexInGroup + direction
        if (itemIndexInGroup in absoluteIndices.indices && targetGroupIndex in absoluteIndices.indices) {
            val absIdx1 = absoluteIndices[itemIndexInGroup]
            val absIdx2 = absoluteIndices[targetGroupIndex]
            val temp = current[absIdx1]
            current[absIdx1] = current[absIdx2]
            current[absIdx2] = temp
            localMediaItems.value = current
        }
    }

    private fun updateFilteredUnits() {
        val allUnits = units.value
        if (allUnits.isEmpty()) {
            filteredUnits.value = emptyList()
            return
        }
        val type = sourceType.value
        val cat = paperCategory.value

        val filtered = if (type == "BOOK") {
            allUnits
        } else {
            when (cat) {
                "DAILY_HW" -> allUnits
                "UNIT_TEST" -> allUnits.filter {
                    val idNum = it.id.toIntOrNull()
                    idNum != null && (idNum <= 12 || idNum == 999)
                }
                "MIDTERM" -> allUnits.filter { it.id == "101" }
                "FINAL" -> allUnits.filter { it.id == "102" }
                "OTHER" -> allUnits.filter { it.id == "999" }
                else -> allUnits
            }
        }
        filteredUnits.value = filtered

        if (filtered.isNotEmpty()) {
            if (!filtered.any { it.id == selectedUnitCode.value }) {
                selectedUnitCode.value = filtered[0].id
            }
        } else {
            selectedUnitCode.value = ""
        }
    }
}
