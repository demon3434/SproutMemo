package com.sproutmemo.android.ui.book

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import org.json.JSONObject

class BookManageViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getService(application)

    // Role & Bound children for Parent view
    val userRole = MutableStateFlow("STUDENT")
    val currentUser = MutableStateFlow<UserDto?>(null)
    val boundChildren = MutableStateFlow<List<UserDto>>(emptyList())
    val selectedChildId = MutableStateFlow<String?>(null)

    // Selection dimensions
    val stages = MutableStateFlow<List<StageDto>>(emptyList())
    val semesters = MutableStateFlow<List<SemesterDto>>(emptyList())
    val filteredSemesters = MutableStateFlow<List<SemesterDto>>(emptyList())
    val subjects = MutableStateFlow<List<SemesterSubjectMappingDto>>(emptyList())
    val books = MutableStateFlow<List<QuestionBookDto>>(emptyList())

    val selectedStageId = MutableStateFlow("")
    val selectedSemesterId = MutableStateFlow("")
    val selectedSubjectId = MutableStateFlow("")
    val newBookName = MutableStateFlow("")
    val studentIdFilter = MutableStateFlow<String?>(null)
    val addError = MutableStateFlow<String?>(null)

    init {
        loadInitData()
    }

    fun loadInitData() {
        viewModelScope.launch {
            try {
                // 1. Fetch current user to determine role
                val userRes = apiService.getCurrentUser()
                if (userRes.isSuccessful && userRes.body()?.code == 200) {
                    val user = userRes.body()?.data
                    currentUser.value = user
                    if (user != null) {
                        userRole.value = user.role

                        // 2. Fetch stages and semesters (independent of role)
                        val stageRes = apiService.getStages()
                        if (stageRes.isSuccessful && stageRes.body()?.code == 200) {
                            stages.value = stageRes.body()?.data?.items ?: emptyList()
                        }
                        val semRes = apiService.getSemesters()
                        if (semRes.isSuccessful && semRes.body()?.code == 200) {
                            semesters.value = semRes.body()?.data?.items ?: emptyList()
                        }

                        if (user.role == "PARENT") {
                            // Fetch bound children
                            val childRes = apiService.getBoundChildren()
                            if (childRes.isSuccessful && childRes.body()?.code == 200) {
                                val list = childRes.body()?.data?.items ?: emptyList()
                                boundChildren.value = list
                                if (list.isNotEmpty()) {
                                    val firstChild = list[0]
                                    selectedChildId.value = firstChild.id
                                    studentIdFilter.value = firstChild.id

                                    // Calculate semester and load subjects / books
                                    val enrollYear = firstChild.student_profile?.enrollment_year ?: firstChild.enrollment_year ?: 2024
                                    val semOffset = firstChild.student_profile?.semester_offset ?: firstChild.semester_offset ?: 0
                                    selectSemesterByProfile(enrollYear, semOffset)
                                }
                            }
                        } else {
                            // STUDENT role
                            studentIdFilter.value = user.id
                            val enrollYear = user.student_profile?.enrollment_year ?: user.enrollment_year ?: 2024
                            val semOffset = user.student_profile?.semester_offset ?: user.semester_offset ?: 0
                            selectSemesterByProfile(enrollYear, semOffset)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun selectSemesterByProfile(enrollmentYear: Int, semesterOffset: Int) {
        val targetSemName = calculateCurrentSemester(enrollmentYear, semesterOffset)
        val allSems = semesters.value
        val matchedSem = allSems.find { it.name == targetSemName }
        if (matchedSem != null) {
            selectedStageId.value = matchedSem.stage_id
            selectedSemesterId.value = matchedSem.id
            updateFilteredSemesters()
            fetchSubjects(matchedSem.id)
        } else {
            // fallback
            if (stages.value.isNotEmpty()) {
                selectedStageId.value = stages.value[0].id
                updateFilteredSemesters()
                val sems = filteredSemesters.value
                if (sems.isNotEmpty()) {
                    selectedSemesterId.value = sems[0].id
                    fetchSubjects(sems[0].id)
                } else {
                    selectedSemesterId.value = ""
                    subjects.value = emptyList()
                    books.value = emptyList()
                }
            }
        }
    }

    private fun updateFilteredSemesters() {
        val stageId = selectedStageId.value
        val allSems = semesters.value
        filteredSemesters.value = allSems.filter { it.stage_id == stageId }
    }

    fun onChildSelected(childId: String) {
        selectedChildId.value = childId
        studentIdFilter.value = childId
        val child = boundChildren.value.find { it.id == childId }
        if (child != null) {
            val enrollYear = child.student_profile?.enrollment_year ?: child.enrollment_year ?: 2024
            val semOffset = child.student_profile?.semester_offset ?: child.semester_offset ?: 0
            selectSemesterByProfile(enrollYear, semOffset)
        }
    }

    fun onStageChanged(stageId: String) {
        selectedStageId.value = stageId
        updateFilteredSemesters()
        val sems = filteredSemesters.value
        if (sems.isNotEmpty()) {
            selectedSemesterId.value = sems[0].id
            fetchSubjects(sems[0].id)
        } else {
            selectedSemesterId.value = ""
            selectedSubjectId.value = ""
            subjects.value = emptyList()
            books.value = emptyList()
        }
    }

    fun onSemesterChanged(semesterId: String) {
        selectedSemesterId.value = semesterId
        if (semesterId.isNotEmpty()) {
            fetchSubjects(semesterId)
        } else {
            selectedSubjectId.value = ""
            subjects.value = emptyList()
            books.value = emptyList()
        }
    }

    fun onSubjectSelected(subjectId: String) {
        selectedSubjectId.value = subjectId
        if (subjectId.isNotEmpty()) {
            fetchBooks(subjectId)
        } else {
            books.value = emptyList()
        }
    }

    fun fetchSubjects(semesterId: String) {
        selectedSemesterId.value = semesterId
        viewModelScope.launch {
            try {
                val subRes = apiService.getSemesterSubjects(semesterId = semesterId)
                if (subRes.isSuccessful && subRes.body()?.code == 200) {
                    val list = subRes.body()?.data?.items ?: emptyList()
                    subjects.value = list
                    if (list.isNotEmpty()) {
                        selectedSubjectId.value = list[0].subject_id
                        fetchBooks(list[0].subject_id)
                    } else {
                        selectedSubjectId.value = ""
                        books.value = emptyList()
                    }
                } else {
                    selectedSubjectId.value = ""
                    subjects.value = emptyList()
                    books.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                selectedSubjectId.value = ""
                subjects.value = emptyList()
                books.value = emptyList()
            }
        }
    }

    fun fetchBooks(subjectId: String) {
        selectedSubjectId.value = subjectId
        viewModelScope.launch {
            try {
                val res = apiService.getQuestionBooks(
                    semesterId = selectedSemesterId.value,
                    subjectId = subjectId,
                    studentId = studentIdFilter.value,
                    onlyActive = false,
                    includeDeleted = false
                )
                val body = res.body()
                if (res.isSuccessful && body?.code == 200) {
                    books.value = body.data?.items ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addBook() {
        addError.value = null
        if (newBookName.value.isBlank()) return
        viewModelScope.launch {
            try {
                val body = mapOf(
                    "name" to newBookName.value.trim(),
                    "semester_id" to selectedSemesterId.value,
                    "subject_id" to selectedSubjectId.value,
                    "student_id" to studentIdFilter.value
                )
                val res = apiService.createQuestionBook(body)
                val bodyData = res.body()
                if (res.isSuccessful && bodyData?.code == 200) {
                    newBookName.value = ""
                    fetchBooks(selectedSubjectId.value)
                } else {
                    val rawError = res.errorBody()?.string()
                    val msg = if (!rawError.isNullOrEmpty()) {
                        try {
                            JSONObject(rawError).optString("msg", "添加失败")
                        } catch (e: Exception) {
                            "添加失败"
                        }
                    } else {
                        res.body()?.msg ?: "添加失败"
                    }
                    addError.value = msg
                }
            } catch (e: Exception) {
                e.printStackTrace()
                addError.value = "网络错误"
            }
        }
    }

    fun moveBook(book: QuestionBookDto, direction: String) {
        viewModelScope.launch {
            try {
                val res = apiService.moveQuestionBook(book.id, direction)
                val body = res.body()
                if (res.isSuccessful && body?.code == 200) {
                    fetchBooks(selectedSubjectId.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun renameBook(bookId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                val book = books.value.find { it.id == bookId }
                val isActive = book?.is_active ?: true
                val request = UpdateBookRequest(
                    name = newName.trim(),
                    is_active = isActive
                )
                val res = apiService.updateQuestionBook(bookId, request)
                val body = res.body()
                if (res.isSuccessful && body?.code == 200) {
                    fetchBooks(selectedSubjectId.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleBookActive(book: QuestionBookDto) {
        viewModelScope.launch {
            try {
                val request = UpdateBookRequest(
                    name = book.name,
                    is_active = !book.is_active
                )
                val res = apiService.updateQuestionBook(book.id, request)
                val body = res.body()
                if (res.isSuccessful && body?.code == 200) {
                    fetchBooks(selectedSubjectId.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            try {
                val res = apiService.deleteQuestionBook(bookId)
                val body = res.body()
                if (res.isSuccessful && body?.code == 200) {
                    fetchBooks(selectedSubjectId.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateCurrentSemester(enrollmentYear: Int, semesterOffset: Int = 0): String {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1 // 1-12

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
