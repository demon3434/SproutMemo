package com.sproutmemo.android.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.DiagnosticLogger
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.data.remote.model.ReviewSessionDto
import com.sproutmemo.android.data.remote.model.StageDto
import com.sproutmemo.android.data.remote.model.SemesterDto
import com.sproutmemo.android.data.remote.model.SemesterSubjectMappingDto
import com.sproutmemo.android.data.remote.model.SubmitReviewItemRequest
import com.sproutmemo.android.data.remote.model.ReviewPresetDataDto
import com.sproutmemo.android.data.remote.model.UnitDto
import com.sproutmemo.android.data.remote.model.RecentStatRowDto
import com.sproutmemo.android.data.remote.model.UserDto
import com.sproutmemo.android.data.remote.model.SubjectDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ReviewState {
    object Initial : ReviewState()
    data class Previewing(val notes: List<NoteDto>) : ReviewState()
    data class Practicing(val session: ReviewSessionDto, val currentIndex: Int) : ReviewState()
    data class Completed(val session: ReviewSessionDto) : ReviewState()
}

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getService(application)

    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Initial)
    val reviewState: StateFlow<ReviewState> = _reviewState

    val activeSessionDetected = MutableStateFlow<ReviewSessionDto?>(null)
    val loading = MutableStateFlow(false)

    val selectedStageId = MutableStateFlow("")
    val selectedSemesterId = MutableStateFlow("")
    val selectedSubjectId = MutableStateFlow("")
    val selectedStrategy = MutableStateFlow("EBBINGHAUS") // EBBINGHAUS, RANDOM, CHALLENGE, PERSISTENT_FAILURE
    val studentIdFilter = MutableStateFlow<String?>(null)

    val stages = MutableStateFlow<List<StageDto>>(emptyList())
    val semesters = MutableStateFlow<List<SemesterDto>>(emptyList())
    val subjects = MutableStateFlow<List<SemesterSubjectMappingDto>>(emptyList())

    val presetsData = MutableStateFlow<ReviewPresetDataDto?>(null)
    val recentStats = MutableStateFlow<List<RecentStatRowDto>>(emptyList())
    val historySessions = MutableStateFlow<List<ReviewSessionDto>>(emptyList())
    val boundChildren = MutableStateFlow<List<UserDto>>(emptyList())
    val selectedChildId = MutableStateFlow<String?>(null)

    val selectedUnitCode = MutableStateFlow<Int?>(null)
    val selectedCountPresetCode = MutableStateFlow("PRESET_5")
    val units = MutableStateFlow<List<UnitDto>>(emptyList())

    val historyYearMonth = MutableStateFlow<String?>(getCurrentYearMonth())
    val historySubjectId = MutableStateFlow<String?>(null)
    val allSubjects = MutableStateFlow<List<SubjectDto>>(emptyList())

    private fun getCurrentYearMonth(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        return "%d-%02d".format(year, month)
    }

    init {
        checkActiveSession()
        loadMetadata()
        loadUnits()
    }

    fun loadAllSubjects() {
        viewModelScope.launch {
            try {
                val res = apiService.getSubjects()
                if (res.isSuccessful && res.body()?.code == 200) {
                    allSubjects.value = res.body()?.data?.items ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadHistorySubjects(semesterId: String) {
        viewModelScope.launch {
            try {
                val res = apiService.getSemesterSubjects(semesterId = semesterId)
                if (res.isSuccessful && res.body()?.code == 200) {
                    allSubjects.value = res.body()?.data?.items?.mapNotNull { it.subject } ?: emptyList()
                } else {
                    loadAllSubjects()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadAllSubjects()
            }
        }
    }

    fun loadMetadata() {
        viewModelScope.launch {
            try {
                val stageRes = apiService.getStages()
                if (stageRes.isSuccessful && stageRes.body()?.code == 200) {
                    stages.value = stageRes.body()?.data?.items ?: emptyList()
                }

                val semRes = apiService.getSemesters()
                val semesterList = if (semRes.isSuccessful && semRes.body()?.code == 200) {
                    semRes.body()?.data?.items ?: emptyList()
                } else {
                    emptyList()
                }
                semesters.value = semesterList

                // Initialize semester based on current student profile
                val userRes = apiService.getCurrentUser()
                if (userRes.isSuccessful && userRes.body()?.code == 200) {
                    val user = userRes.body()?.data
                    if (user != null) {
                        if (user.role == "STUDENT") {
                            val enrollmentYear = user.student_profile?.enrollment_year ?: user.enrollment_year
                            val semesterOffset = user.student_profile?.semester_offset ?: user.semester_offset ?: 0
                            initializeFiltersForStudent(enrollmentYear, semesterOffset)
                        } else if (user.role == "PARENT") {
                            // Parent will load via loadBoundChildren -> onChildSelected
                        } else {
                            loadAllSubjects()
                        }
                    }
                } else {
                    loadAllSubjects()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadAllSubjects()
            }
        }
    }

    fun onSemesterSelected(semesterId: String) {
        selectedSemesterId.value = semesterId
        selectedSubjectId.value = ""
        subjects.value = emptyList()

        if (semesterId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    val subRes = apiService.getSemesterSubjects(semesterId = semesterId)
                    if (subRes.isSuccessful && subRes.body()?.code == 200) {
                        subjects.value = subRes.body()?.data?.items ?: emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun checkActiveSession() {
        viewModelScope.launch {
            try {
                val res = apiService.getActiveReviewSession(studentId = studentIdFilter.value)
                if (res.isSuccessful && res.body()?.code == 200 && res.body()?.data != null) {
                    val active = res.body()?.data
                    if (active != null && active.status == "IN_PROGRESS") {
                        activeSessionDetected.value = active
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generatePreview() {
        val semId = selectedSemesterId.value
        if (semId.isBlank()) {
            DiagnosticLogger.log("Review", "W", "generatePreview skipped because semesterId is blank")
            return
        }
        viewModelScope.launch {
            loading.value = true
            try {
                val subId = selectedSubjectId.value.ifBlank { null }
                val strategy = selectedStrategy.value
                val countPreset = selectedCountPresetCode.value
                val studentId = studentIdFilter.value
                val unitCode = selectedUnitCode.value

                DiagnosticLogger.log("Review", "I", "Generating review preview: semester=$semId, subject=$subId, strategy=$strategy, count=$countPreset")
                val res = apiService.generateReviewPreview(
                    semesterId = semId,
                    subjectId = subId,
                    unitCode = unitCode,
                    strategyCode = strategy,
                    countPresetCode = countPreset,
                    studentId = studentId
                )
                if (res.isSuccessful && res.body()?.code == 200) {
                    val notes = res.body()?.data ?: emptyList()
                    _reviewState.value = ReviewState.Previewing(notes)
                    DiagnosticLogger.log("Review", "I", "Review preview generated successfully, notes size=${notes.size}")
                } else {
                    val err = res.errorBody()?.string() ?: res.body()?.msg
                    DiagnosticLogger.log("Review", "E", "Generate review preview failed: code=${res.code()}, err=$err")
                }
            } catch (e: Exception) {
                DiagnosticLogger.log("Review", "E", "generatePreview exception: ${e.localizedMessage}", e)
                e.printStackTrace()
            } finally {
                loading.value = false
            }
        }
    }

    fun startFormalSession(previewNotes: List<NoteDto>) {
        val semId = selectedSemesterId.value
        if (semId.isBlank()) return
        viewModelScope.launch {
            loading.value = true
            try {
                val subId = selectedSubjectId.value.ifBlank { null }
                val strategy = selectedStrategy.value
                val countPreset = selectedCountPresetCode.value
                val noteIds = previewNotes.map { it.id }
                val unitCode = selectedUnitCode.value
 
                val body = mapOf<String, Any?>(
                    "semester_id" to semId,
                    "subject_id" to subId,
                    "unit_code" to unitCode,
                    "strategy_code" to strategy,
                    "count_preset_code" to countPreset,
                    "student_id" to studentIdFilter.value,
                    "note_ids" to noteIds
                )
                DiagnosticLogger.log("Review", "I", "Creating formal review session: semester=$semId, noteCount=${noteIds.size}")
                val res = apiService.createReviewSession(body)
                if (res.isSuccessful && res.body()?.code == 200 && res.body()?.data != null) {
                    val session = res.body()!!.data!!
                    _reviewState.value = ReviewState.Practicing(session, 0)
                    DiagnosticLogger.log("Review", "I", "Formal review session created successfully: id=${session.id}")
                } else {
                    val err = res.errorBody()?.string() ?: res.body()?.msg
                    DiagnosticLogger.log("Review", "E", "Create review session failed: code=${res.code()}, err=$err")
                }
            } catch (e: Exception) {
                DiagnosticLogger.log("Review", "E", "startFormalSession exception: ${e.localizedMessage}", e)
                e.printStackTrace()
            } finally {
                loading.value = false
            }
        }
    }

    fun resumeSession(session: ReviewSessionDto) {
        activeSessionDetected.value = null
        _reviewState.value = ReviewState.Practicing(session, session.completed_count)
    }

    fun abandonSession(sessionId: String) {
        viewModelScope.launch {
            try {
                apiService.abandonReviewSession(sessionId)
                activeSessionDetected.value = null
                _reviewState.value = ReviewState.Initial
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun submitAnswer(
        session: ReviewSessionDto,
        noteId: String,
        outcomeCode: String,
        newMastery: String,
        newStars: Double,
        excludeFromReview: Boolean,
        durationSeconds: Double,
        sequenceIndex: Int,
        onSuccess: () -> Unit = {},
        onError: (String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            loading.value = true
            try {
                val request = SubmitReviewItemRequest(
                    noteId = noteId,
                    durationSeconds = durationSeconds,
                    outcomeCode = outcomeCode,
                    newMastery = newMastery,
                    newStars = newStars,
                    excludeFromReview = excludeFromReview,
                    sequenceIndex = sequenceIndex
                )
                DiagnosticLogger.log("Review", "I", "Submitting answer: sessionId=${session.id}, noteId=$noteId, outcome=$outcomeCode")
                val res = apiService.submitReviewItem(session.id, request)
                if (res.isSuccessful && res.body()?.code == 200 && res.body()?.data != null) {
                    val updated = res.body()!!.data!!
                    if (updated.status == "COMPLETED") {
                        _reviewState.value = ReviewState.Completed(updated)
                        DiagnosticLogger.log("Review", "I", "Review session completed: id=${updated.id}")
                    } else {
                        _reviewState.value = ReviewState.Practicing(updated, updated.completed_count)
                        DiagnosticLogger.log("Review", "I", "Review item submitted: completed_count=${updated.completed_count}")
                    }
                    onSuccess()
                } else {
                    val err = res.errorBody()?.string() ?: res.body()?.msg
                    DiagnosticLogger.log("Review", "E", "Submit review item failed: code=${res.code()}, err=$err")
                    onError(err)
                }
            } catch (e: Exception) {
                DiagnosticLogger.log("Review", "E", "submitAnswer exception: ${e.localizedMessage}", e)
                e.printStackTrace()
                onError(e.localizedMessage)
            } finally {
                loading.value = false
            }
        }
    }

    fun loadUnits() {
        viewModelScope.launch {
            try {
                val res = apiService.getUnits()
                if (res.isSuccessful && res.body()?.code == 200) {
                    units.value = res.body()?.data?.items ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadReviewPresets() {
        viewModelScope.launch {
            try {
                val studentId = studentIdFilter.value ?: selectedChildId.value
                val res = apiService.getReviewPresets(studentId = studentId)
                if (res.isSuccessful && res.body()?.code == 200) {
                    presetsData.value = res.body()?.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadRecentStats() {
        viewModelScope.launch {
            try {
                val studentId = studentIdFilter.value ?: selectedChildId.value
                val res = apiService.getRecentReviewStats(studentId = studentId)
                if (res.isSuccessful && res.body()?.code == 200) {
                    recentStats.value = res.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadReviewHistory() {
        viewModelScope.launch {
            try {
                val studentId = studentIdFilter.value ?: selectedChildId.value
                val res = apiService.getReviewHistory(
                    limit = 100,
                    offset = 0,
                    subjectId = historySubjectId.value,
                    studentId = studentId,
                    yearMonth = historyYearMonth.value
                )
                if (res.isSuccessful && res.body()?.code == 200) {
                    historySessions.value = res.body()?.data?.items ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadBoundChildren(userRole: String) {
        if (userRole != "PARENT") return
        viewModelScope.launch {
            try {
                val res = apiService.getBoundChildren()
                if (res.isSuccessful && res.body()?.code == 200) {
                    val list = res.body()?.data?.items ?: emptyList()
                    boundChildren.value = list
                    if (list.isNotEmpty() && selectedChildId.value == null) {
                        onChildSelected(list[0].id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onChildSelected(childId: String) {
        selectedChildId.value = childId
        loadReviewPresets()
        loadRecentStats()
        loadReviewHistory()

        val child = boundChildren.value.find { it.id == childId }
        if (child != null) {
            val enrollmentYear = child.student_profile?.enrollment_year ?: child.enrollment_year
            val semesterOffset = child.student_profile?.semester_offset ?: child.semester_offset ?: 0
            initializeFiltersForStudent(enrollmentYear, semesterOffset)
        }
    }

    private fun calculateCurrentSemester(enrollmentYear: Int, semesterOffset: Int = 0): String {
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

    private fun initializeFiltersForStudent(enrollmentYear: Int?, semesterOffset: Int?) {
        if (enrollmentYear == null) return
        val targetSemName = calculateCurrentSemester(enrollmentYear, semesterOffset ?: 0)
        val matchedSemester = semesters.value.find { it.name == targetSemName }
        if (matchedSemester != null) {
            selectedSemesterId.value = matchedSemester.id
            selectedStageId.value = matchedSemester.stage_id ?: ""
            onSemesterSelected(matchedSemester.id)
            loadHistorySubjects(matchedSemester.id)
        } else {
            loadAllSubjects()
        }
    }

    fun setPracticeIndex(session: ReviewSessionDto, index: Int) {
        _reviewState.value = ReviewState.Practicing(session, index)
    }

    fun loadSessionForPractice(sessionId: String) {
        val currentState = _reviewState.value
        if (currentState is ReviewState.Practicing && currentState.session.id == sessionId) {
            return
        }

        viewModelScope.launch {
            loading.value = true
            try {
                DiagnosticLogger.log("Review", "I", "Loading session for practice: $sessionId")
                val res = apiService.getReviewSessionDetails(sessionId)
                if (res.isSuccessful && res.body()?.code == 200 && res.body()?.data != null) {
                    val session = res.body()!!.data!!
                    val index = session.notes.orEmpty().indexOfFirst { !it.is_completed }.coerceAtLeast(0)
                    _reviewState.value = ReviewState.Practicing(session, index)
                    DiagnosticLogger.log("Review", "I", "Session loaded for practice, notes count=${session.notes.orEmpty().size}, next index=$index")
                } else {
                    val err = res.errorBody()?.string() ?: res.body()?.msg
                    DiagnosticLogger.log("Review", "E", "Load session for practice failed: code=${res.code()}, err=$err")
                }
            } catch (e: Exception) {
                DiagnosticLogger.log("Review", "E", "loadSessionForPractice exception: ${e.localizedMessage}", e)
                e.printStackTrace()
            } finally {
                loading.value = false
            }
        }
    }

    fun resetReview() {
        _reviewState.value = ReviewState.Initial
    }
}
