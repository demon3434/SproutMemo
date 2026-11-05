package com.sproutmemo.android.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.RecentStatRowDto
import com.sproutmemo.android.data.remote.model.StatsOverviewDto
import com.sproutmemo.android.data.remote.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getService(application)
    private val tokenManager = ApiClient.getTokenManager(application)
    private val gson = Gson()

    val loading = MutableStateFlow(false)
    val userRole = MutableStateFlow("STUDENT")
    val boundStudents = MutableStateFlow<List<UserDto>>(emptyList())
    val selectedStudentId = MutableStateFlow<String?>(null)
    val statsOverview = MutableStateFlow<StatsOverviewDto?>(null)
    val recentStats = MutableStateFlow<List<RecentStatRowDto>>(emptyList())

    init {
        loadUserContext()
    }

    private fun loadUserContext() {
        viewModelScope.launch {
            val role = tokenManager.getUserRoleSync() ?: "STUDENT"
            userRole.value = role

            if (role == "PARENT") {
                val boundJson = tokenManager.boundStudentsJsonFlow.first()
                if (!boundJson.isNullOrEmpty()) {
                    val type = object : TypeToken<List<UserDto>>() {}.type
                    val list: List<UserDto> = gson.fromJson(boundJson, type)
                    boundStudents.value = list
                    if (list.isNotEmpty()) {
                        selectedStudentId.value = list[0].id
                    }
                }
            }

            fetchDashboardData()
        }
    }

    fun selectStudent(studentId: String) {
        selectedStudentId.value = studentId
        fetchDashboardData()
    }

    fun fetchDashboardData() {
        viewModelScope.launch {
            loading.value = true
            try {
                val studentId = selectedStudentId.value
                val presetRes = apiService.getReviewPresets(studentId = studentId)
                if (presetRes.isSuccessful && presetRes.body()?.code == 200) {
                    statsOverview.value = presetRes.body()?.data?.stats
                }

                val recentRes = apiService.getRecentReviewStats(studentId = studentId)
                if (recentRes.isSuccessful && recentRes.body()?.code == 200) {
                    recentStats.value = recentRes.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading.value = false
            }
        }
    }
}
