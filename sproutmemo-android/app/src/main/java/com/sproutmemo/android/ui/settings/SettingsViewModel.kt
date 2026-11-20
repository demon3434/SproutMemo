package com.sproutmemo.android.ui.settings

import android.app.Application
import android.widget.Toast
import android.content.Context
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.DiagnosticLogger
import com.sproutmemo.android.data.remote.model.*
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.gridPaper
import com.sproutmemo.android.ui.theme.ThemeList
import com.sproutmemo.android.ui.theme.LocalAppColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiClient.getService(application)
    private val tokenManager = ApiClient.getTokenManager(application)

    val currentUser = MutableStateFlow<UserDto?>(null)
    val oldPassword = MutableStateFlow("")
    val newPassword = MutableStateFlow("")
    val confirmPassword = MutableStateFlow("")
    val statusMessage = MutableStateFlow<String?>(null)
    val themeId = MutableStateFlow(18)
    val videoQuality = MutableStateFlow("720p")

    init {
        loadUser()
        loadTheme()
        loadVideoQuality()
    }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val res = apiService.getCurrentUser()
                if (res.isSuccessful && res.body()?.code == 200) {
                    currentUser.value = res.body()?.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadTheme() {
        viewModelScope.launch {
            themeId.value = tokenManager.themeIdFlow.first()
        }
    }

    private fun loadVideoQuality() {
        viewModelScope.launch {
            videoQuality.value = tokenManager.videoQualityFlow.first()
        }
    }

    fun selectTheme(id: Int) {
        viewModelScope.launch {
            tokenManager.saveThemeId(id)
            themeId.value = id
        }
    }

    fun selectVideoQuality(quality: String) {
        viewModelScope.launch {
            tokenManager.saveVideoQuality(quality)
            videoQuality.value = quality
        }
    }

    fun submitChangePassword() {
        if (newPassword.value.length < 6) {
            statusMessage.value = "新密码长度不能少于6位"
            return
        }
        if (newPassword.value != confirmPassword.value) {
            statusMessage.value = "两次输入的新密码不一致"
            return
        }
        viewModelScope.launch {
            try {
                val res = apiService.updateProfile(
                    UpdateProfileRequest(password = newPassword.value)
                )
                if (res.isSuccessful && res.body()?.code == 200) {
                    statusMessage.value = "密码修改成功！"
                    oldPassword.value = ""
                    newPassword.value = ""
                    confirmPassword.value = ""
                } else {
                    statusMessage.value = res.body()?.msg ?: "修改失败"
                }
            } catch (e: Exception) {
                statusMessage.value = "网络请求异常: ${e.localizedMessage}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAuth()
        }
    }
}

