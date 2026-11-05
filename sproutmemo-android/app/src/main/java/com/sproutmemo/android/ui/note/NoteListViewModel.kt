package com.sproutmemo.android.ui.note

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class NoteListViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getService(application)

    val notes = MutableStateFlow<List<NoteDto>>(emptyList())
    val loading = MutableStateFlow(false)
    val page = MutableStateFlow(1)
    val totalPages = MutableStateFlow(1)
    val totalItems = MutableStateFlow(0)

    val searchQuery = MutableStateFlow("")
    val selectedSubjectId = MutableStateFlow("")
    val selectedStageId = MutableStateFlow("")
    val selectedSemesterId = MutableStateFlow("")
    val selectedMastery = MutableStateFlow("")
    val selectedUnitCode = MutableStateFlow("")
    val selectedDifficulty = MutableStateFlow("")
    val sourceType = MutableStateFlow("")
    val studentId = MutableStateFlow<String?>(null)
    val boundChildren = MutableStateFlow<List<UserDto>>(emptyList())

    val stages = MutableStateFlow<List<StageDto>>(emptyList())
    val semesters = MutableStateFlow<List<SemesterDto>>(emptyList())
    val subjects = MutableStateFlow<List<SemesterSubjectMappingDto>>(emptyList())
    val systemUnits = MutableStateFlow<List<UnitDto>>(emptyList())
    val selectedNoteDetail = MutableStateFlow<NoteDto?>(null)

    init {
        loadMetadata()
    }

    fun loadMetadata() {
        viewModelScope.launch {
            try {
                val stageRes = apiService.getStages()
                if (stageRes.isSuccessful && stageRes.body()?.code == 200) {
                    stages.value = stageRes.body()?.data?.items ?: emptyList()
                }

                val semRes = apiService.getSemesters()
                if (semRes.isSuccessful && semRes.body()?.code == 200) {
                    semesters.value = semRes.body()?.data?.items ?: emptyList()
                }

                val unitRes = apiService.getUnits()
                if (unitRes.isSuccessful && unitRes.body()?.code == 200) {
                    systemUnits.value = unitRes.body()?.data?.items ?: emptyList()
                }
                // Note: do NOT call onSemesterSelected("") here.
                // initializeDefaultFilters will be called reactively after semesters load,
                // and onSemesterSelected("") would race with it and reset the semester selection.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onSemesterSelected(semesterId: String) {
        selectedSemesterId.value = semesterId
        selectedSubjectId.value = ""
        subjects.value = emptyList()

        viewModelScope.launch {
            try {
                if (semesterId.isNotBlank()) {
                    val subRes = apiService.getSemesterSubjects(semesterId = semesterId)
                    if (subRes.isSuccessful && subRes.body()?.code == 200) {
                        subjects.value = subRes.body()?.data?.items ?: emptyList()
                    }
                } else {
                    val subRes = apiService.getSubjects()
                    if (subRes.isSuccessful && subRes.body()?.code == 200) {
                        subjects.value = subRes.body()?.data?.items?.map { sub ->
                            SemesterSubjectMappingDto(
                                id = "",
                                semester_id = "",
                                subject_id = sub.subject_id,
                                subject = sub,
                                display_order = sub.display_order
                            )
                        } ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setStudentIdFilter(id: String?) {
        studentId.value = id
        fetchNotes(resetPage = true)
    }

    fun fetchNotes(resetPage: Boolean = false) {
        if (resetPage) page.value = 1
        viewModelScope.launch {
            loading.value = true
            try {
                val res = apiService.getNotes(
                    page = page.value,
                    pageSize = 10,
                    semesterId = selectedSemesterId.value.ifBlank { null },
                    subjectId = selectedSubjectId.value.ifBlank { null },
                    masteryLevel = selectedMastery.value.ifBlank { null },
                    unitCode = selectedUnitCode.value.ifBlank { null },
                    sourceType = sourceType.value.ifBlank { null },
                    difficultyStars = selectedDifficulty.value.toIntOrNull(),
                    studentId = studentId.value
                )

                if (res.isSuccessful && res.body()?.code == 200) {
                    val paginated = res.body()?.data
                    if (paginated != null) {
                        notes.value = paginated.items
                        totalPages.value = paginated.total_pages
                        totalItems.value = paginated.total
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading.value = false
            }
        }
    }

    fun applyUpdatedNote(updatedNote: NoteDto) {
        if (selectedNoteDetail.value?.id == updatedNote.id) {
            selectedNoteDetail.value = updatedNote
        }
        notes.value = notes.value.map { if (it.id == updatedNote.id) updatedNote else it }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                val res = apiService.deleteNote(noteId)
                if (res.isSuccessful && res.body()?.code == 200) {
                    fetchNotes()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateNoteFields(
        noteId: String,
        difficultyStars: Float? = null,
        masteryLevel: String? = null,
        excludeFromReview: Boolean? = null,
        onSuccess: (NoteDto) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val now = java.time.Instant.now().toString()
                val fields = mutableMapOf<String, okhttp3.RequestBody>(
                    "sync_base_time" to now.toRequestBody("text/plain".toMediaTypeOrNull()),
                    "force" to "true".toRequestBody("text/plain".toMediaTypeOrNull())
                )
                difficultyStars?.let {
                    fields["difficulty_stars"] = it.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                }
                masteryLevel?.let {
                    fields["mastery_level"] = it.toRequestBody("text/plain".toMediaTypeOrNull())
                }
                excludeFromReview?.let {
                    fields["exclude_from_review"] = it.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                }
                val res = apiService.updateNote(noteId, fields, emptyList())
                if (res.isSuccessful && res.body()?.code == 200) {
                    val updatedNote = res.body()!!.data!!
                    // Update selectedNoteDetail
                    if (selectedNoteDetail.value?.id == noteId) {
                        selectedNoteDetail.value = updatedNote
                    }
                    // Update in the notes list
                    notes.value = notes.value.map { if (it.id == noteId) updatedNote else it }
                    onSuccess(updatedNote)
                } else {
                    onError(res.body()?.msg ?: "更新失败")
                }
            } catch (e: Exception) {
                onError(e.message ?: "网络错误")
            }
        }
    }

    private var lastInitializedStudentId: String? = "uninitialized"

    fun initializeDefaultFilters(role: String, targetStudentId: String?) {
        viewModelScope.launch {
            try {
                // Wait for semesters to be loaded
                var attempts = 0
                while (semesters.value.isEmpty() && attempts < 50) {
                    kotlinx.coroutines.delay(100)
                    attempts++
                }

                var enrollYear: Int? = null
                var semOffset: Int? = null
                var activeChildId: String? = targetStudentId

                if (role == "STUDENT") {
                    val meRes = apiService.getCurrentUser()
                    if (meRes.isSuccessful && meRes.body()?.code == 200) {
                        val u = meRes.body()?.data
                        enrollYear = u?.enrollment_year ?: u?.student_profile?.enrollment_year
                        semOffset = u?.semester_offset ?: u?.student_profile?.semester_offset ?: 0
                    }
                } else if (role == "PARENT") {
                    val childrenRes = apiService.getBoundChildren()
                    if (childrenRes.isSuccessful && childrenRes.body()?.code == 200) {
                        val list = childrenRes.body()?.data?.items ?: emptyList()
                        boundChildren.value = list
                        activeChildId = targetStudentId ?: studentId.value ?: list.firstOrNull()?.id
                        studentId.value = activeChildId
                        val child = list.find { it.id == activeChildId }
                        enrollYear = child?.enrollment_year ?: child?.student_profile?.enrollment_year
                        semOffset = child?.semester_offset ?: child?.student_profile?.semester_offset ?: 0
                    }
                }

                if (lastInitializedStudentId == activeChildId && activeChildId != null) {
                    fetchNotes(resetPage = true)
                    return@launch
                }
                lastInitializedStudentId = activeChildId

                if (enrollYear != null) {
                    val calculatedName = calculateCurrentSemesterName(enrollYear, semOffset ?: 0)
                    val matchingSem = semesters.value.find { it.name == calculatedName }
                    if (matchingSem != null) {
                        selectedSemesterId.value = matchingSem.id
                        selectedStageId.value = matchingSem.stage_id
                        onSemesterSelected(matchingSem.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fetchNotes(resetPage = true)
        }
    }

    private fun calculateCurrentSemesterName(enrollmentYear: Int, semesterOffset: Int): String {
        val now = java.util.Calendar.getInstance()
        val currentYear = now.get(java.util.Calendar.YEAR)
        val currentMonth = now.get(java.util.Calendar.MONTH) + 1 // 1-12

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
}
