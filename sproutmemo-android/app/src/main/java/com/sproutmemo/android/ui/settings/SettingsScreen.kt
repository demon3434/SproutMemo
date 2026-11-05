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
    val themeId = MutableStateFlow(8)
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

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onRegisterBackHandler: ((() -> Unit)?) -> Unit = {}
) {
    val user by viewModel.currentUser.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentThemeId by viewModel.themeId.collectAsState()
    val colors = LocalAppColors.current

    var showDebugLogs by remember { mutableStateOf(false) }
    var showThemeSettings by remember { mutableStateOf(false) }
    var showVideoQuality by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showServerConfig by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val subPageBackHandler = remember(showDebugLogs, showVideoQuality, showChangePassword, showServerConfig, showThemeSettings) {
        if (showDebugLogs || showVideoQuality || showChangePassword || showServerConfig || showThemeSettings) {
            {
                showDebugLogs = false
                showVideoQuality = false
                showChangePassword = false
                showServerConfig = false
                showThemeSettings = false
            }
        } else {
            null
        }
    }

    LaunchedEffect(subPageBackHandler) {
        onRegisterBackHandler(subPageBackHandler)
    }

    BackHandler(enabled = subPageBackHandler != null) {
        subPageBackHandler?.invoke()
    }

    if (showDebugLogs) {
        DebugLogView(onBack = { showDebugLogs = false })
        return
    }

    if (showThemeSettings) {
        ThemeSettingsScreen(viewModel = viewModel, onBack = { showThemeSettings = false })
        return
    }

    if (showVideoQuality) {
        VideoQualitySettingsScreen(viewModel = viewModel, onBack = { showVideoQuality = false })
        return
    }

    if (showChangePassword) {
        ChangePasswordScreen(viewModel = viewModel, onBack = { showChangePassword = false })
        return
    }

    if (showServerConfig) {
        ServerConfigScreen(onBack = { showServerConfig = false })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.darkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("⚙️ 个人设置与安全", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Current logged in account info
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("👤 当前登录账号信息", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("账号：${user?.username ?: "-"}", fontSize = 13.sp, color = colors.textPrimary)
                Text("姓名：${user?.nickname ?: user?.username ?: "-"}", fontSize = 13.sp, color = colors.textPrimary)
                Text("角色：${user?.role ?: "-"}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.neonGreen)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Change password
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showChangePassword = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔐", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("修改密码", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("简化流程，快速重置您的登录密码", fontSize = 11.sp, color = colors.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Configure server address
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showServerConfig = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🌐", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("配置服务地址", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("设置并测试内网与外网连接基准", fontSize = 11.sp, color = colors.textSecondary)
                }
            }
        }

        if (user?.role == "STUDENT") {
            Spacer(modifier = Modifier.height(12.dp))

            // 4. Video upload compression (Student only)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showVideoQuality = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeQuality = when (viewModel.videoQuality.collectAsState().value) {
                        "360p" -> "极小体积 (360P)"
                        "480p" -> "标清流畅 (480P)"
                        "720p" -> "高清标准 (720P)"
                        "1080p" -> "超清画质 (1080P)"
                        else -> "高清标准 (720P)"
                    }
                    Text("🎬", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("视频上传压缩设置", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("当前品质: $activeQuality", fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Personalized theme color scheme
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showThemeSettings = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎨", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val activeThemeName = ThemeList.find { it.id == currentThemeId }?.name ?: "默认"
                    Text("个性化主题配色", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("当前方案: $activeThemeName", fontSize = 11.sp, color = colors.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. System Diagnostics (Always Last!)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDebugLogs = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💻", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("系统诊断日志", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("查看调试、网络和附件加载状态", fontSize = 11.sp, color = colors.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                showLogoutConfirm = true
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
        ) {
            Text("🚪 退出登录", fontWeight = FontWeight.Bold)
        }
    }

    if (statusMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.statusMessage.value = null },
            confirmButton = { TextButton(onClick = { viewModel.statusMessage.value = null }) { Text("确定") } },
            text = { Text(statusMessage ?: "") }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("确认退出", fontWeight = FontWeight.Bold) },
            text = { Text("您确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Text("确定", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun DebugLogView(onBack: () -> Unit) {
    val context = LocalContext.current
    var logsText by remember { mutableStateOf(DiagnosticLogger.getLogs()) }
    var filterText by remember { mutableStateOf("") }

    val filteredLogs = remember(logsText, filterText) {
        if (filterText.isBlank()) {
            logsText
        } else {
            logsText.split("\n")
                .filter { it.contains(filterText, ignoreCase = true) }
                .joinToString("\n")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💻 诊断调试日志", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Logs", filteredLogs)
                    clipboard.setPrimaryClip(clip)
                }) {
                    Text("复制", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = {
                    DiagnosticLogger.clear()
                    logsText = ""
                }) {
                    Text("清空", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            placeholder = { Text("过滤日志...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            val scrollState = rememberScrollState()
            Text(
                text = if (filteredLogs.isBlank()) "📄 暂无日志数据..." else filteredLogs,
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            )
        }
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun ChangePasswordScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val newPassword by viewModel.newPassword.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.darkBg)
            .padding(16.dp)
    ) {
        // Top Back Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("🔐 修改密码", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("请填写以下信息以修改您的密码", fontSize = 13.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(16.dp))

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedLabelColor = colors.neonBlue,
                    unfocusedLabelColor = colors.textSecondary,
                    focusedBorderColor = colors.neonBlue,
                    unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.5f),
                    cursorColor = colors.neonBlue
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { viewModel.newPassword.value = it },
                    label = { Text("新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.confirmPassword.value = it },
                    label = { Text("确认新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMessage ?: "",
                        color = if (statusMessage?.contains("成功") == true) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.submitChangePassword() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                ) {
                    Text("提交修改密码", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ThemeSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val currentThemeId by viewModel.themeId.collectAsState()
    val context = LocalContext.current
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.darkBg)
            .padding(16.dp)
    ) {
        // Top Row Title only
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("🎨 个性化主题配色", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "选择您喜欢的系统主题配色方案：",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemeList.forEach { theme ->
                        val isSelected = theme.id == currentThemeId
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (theme.isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)),
                            border = if (isSelected) borderStroke(2.dp, colors.neonBlue) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectTheme(theme.id)
                                    Toast.makeText(context, "主题配色已更新为：${theme.name}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(theme.darkBg)
                                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(theme.neonBlue)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = theme.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (theme.isDark) Color.White else Color.Black
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(theme.neonBlue))
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(theme.neonGreen))
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(theme.neonAmber))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
