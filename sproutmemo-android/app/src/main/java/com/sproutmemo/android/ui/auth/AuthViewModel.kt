package com.sproutmemo.android.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sproutmemo.android.data.local.TokenManager
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.NetworkProbe
import com.sproutmemo.android.data.remote.NsdHelper
import com.sproutmemo.android.data.remote.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String, val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = ApiClient.getTokenManager(application)
    private val gson = Gson()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val intranetUrl = MutableStateFlow("")
    val extranetUrl = MutableStateFlow("")
    val activeServerUrl = MutableStateFlow("")
    val isDiscovering = MutableStateFlow(false)

    init {
        loadServerUrls()
        checkAutoLogin()
    }

    private fun loadServerUrls() {
        viewModelScope.launch {
            intranetUrl.value = tokenManager.getIntranetUrlSync()
            extranetUrl.value = tokenManager.getExtranetUrlSync()
            activeServerUrl.value = tokenManager.getActiveServerUrlSync()
        }
    }

    fun checkAutoLogin() {
        viewModelScope.launch {
            val accessToken = tokenManager.getAccessTokenSync()
            val role = tokenManager.getUserRoleSync()
            val userId = tokenManager.getUserIdSync()
            if (!accessToken.isNullOrEmpty() && !role.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                if (role == "ADMIN") {
                    tokenManager.clearAuth()
                    _authState.value = AuthState.Idle
                } else {
                    _authState.value = AuthState.Success(role, userId)
                }
            }
        }
    }

    fun discoverLanServer(onDiscovered: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isDiscovering.value = true
            try {
                val found = NsdHelper.discoverServer(getApplication())
                if (found != null) {
                    onDiscovered(found)
                } else {
                    onError("未在局域网内扫描到 SproutMemo 服务 (mDNS)，请手动输入 IP 地址")
                }
            } catch (e: Exception) {
                onError("mDNS 扫描异常: ${e.localizedMessage}")
            } finally {
                isDiscovering.value = false
            }
        }
    }

    fun login(usernameInput: String, passwordInput: String, intraUrl: String, extraUrl: String) {
        val username = usernameInput.trim()
        val password = passwordInput

        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("请输入用户名和密码")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // 1. Network Probe & Cascade Discovery
            val selectedUrl = NetworkProbe.discoverAndSelectUrl(getApplication(), intraUrl, extraUrl)
            if (selectedUrl == null) {
                _authState.value = AuthState.Error("无法连接到服务器，请检查内外网 URL 配置或网络状态")
                return@launch
            }

            tokenManager.saveServerUrls(intraUrl, extraUrl, selectedUrl)
            ApiClient.updateBaseUrl(getApplication())
            activeServerUrl.value = selectedUrl

            // 2. Perform Login
            try {
                val apiService = ApiClient.getService(getApplication())
                val res = apiService.login(mapOf("username" to username, "password" to password))

                if (res.isSuccessful && res.body()?.code == 200) {
                    val data = res.body()?.data
                    if (data != null) {
                        val accessToken = data["access_token"] as? String ?: ""
                        val refreshToken = data["refresh_token"] as? String ?: ""
                        val userObj = data["user"]
                        val userDto = gson.fromJson(gson.toJson(userObj), UserDto::class.java)

                        // 🔴 Role Gate: Reject ADMIN logins on Android
                        if (userDto.role == "ADMIN") {
                            _authState.value = AuthState.Error("管理员账号仅支持 Web 端登录，手机/Pad 客户端仅供家长和学生使用")
                            return@launch
                        }

                        tokenManager.saveTokens(accessToken, refreshToken)
                        val boundJson = if (userDto.bound_students != null) gson.toJson(userDto.bound_students) else null
                        tokenManager.saveUserInfo(
                            userId = userDto.id,
                            role = userDto.role,
                            username = userDto.username,
                            nickname = userDto.nickname,
                            boundStudentsJson = boundJson
                        )

                        _authState.value = AuthState.Success(userDto.role, userDto.id)
                    } else {
                        _authState.value = AuthState.Error("登录数据格式错误")
                    }
                } else {
                    // Extract real error message from HTTP errorBody
                    val rawError = res.errorBody()?.string()
                    val errorMsg = try {
                        if (!rawError.isNullOrEmpty()) {
                            val json = gson.fromJson(rawError, Map::class.java)
                            (json["msg"] as? String) ?: (json["detail"] as? String) ?: "登录错误 (${res.code()})"
                        } else {
                            "登录错误 (${res.code()})"
                        }
                    } catch (e: Exception) {
                        "登录失败 (${res.code()})"
                    }
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("网络连接失败: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAuth()
            _authState.value = AuthState.Idle
        }
    }
}
