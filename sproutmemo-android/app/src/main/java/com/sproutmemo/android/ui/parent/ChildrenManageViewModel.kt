package com.sproutmemo.android.ui.parent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ChildrenManageViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getService(application)

    val children = MutableStateFlow<List<UserDto>>(emptyList())
    val loading = MutableStateFlow(false)

    init {
        fetchChildren()
    }

    fun fetchChildren() {
        viewModelScope.launch {
            loading.value = true
            try {
                val res = apiService.getBoundChildren()
                if (res.isSuccessful && res.body()?.code == 200) {
                    children.value = res.body()?.data?.items ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading.value = false
            }
        }
    }

    fun changeSemesterOffset(childId: String, currentOffset: Int, delta: Int) {
        val newOffset = currentOffset + delta
        viewModelScope.launch {
            try {
                val res = apiService.updateSemesterOffset(childId, mapOf("semester_offset" to newOffset))
                if (res.isSuccessful && res.body()?.code == 200) {
                    fetchChildren()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun calculateSemesterName(enrollmentYear: Int?, offset: Int?): String {
        if (enrollmentYear == null) return "未知学期"
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-12

        var gradeNum = currentYear - enrollmentYear + 1
        var term = if (currentMonth in 3..8) "下学期" else "上学期"
        if (currentMonth in 9..12) gradeNum = currentYear - enrollmentYear + 1

        val gradeName = when (gradeNum) {
            1 -> "一年级"
            2 -> "二年级"
            3 -> "三年级"
            4 -> "四年级"
            5 -> "五年级"
            6 -> "六年级"
            7 -> "七年级"
            8 -> "八年级"
            9 -> "九年级"
            else -> "高中/其它"
        }

        return "$gradeName $term"
    }
}
