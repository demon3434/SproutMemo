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
            .gridPaper(colors.darkBg, colors.rule)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StationeryIcon(name = "settings", size = 22.dp, tint = colors.neonBlue)
            Text("个人设置与安全", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Current logged in account info
        StationeryCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StationeryIcon(name = "user", size = 16.dp, tint = colors.neonBlue)
                    Text("当前登录账号信息", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("账号：${user?.username ?: "-"}", fontSize = 13.sp, color = colors.textPrimary)
                Text("姓名：${user?.nickname ?: user?.username ?: "-"}", fontSize = 13.sp, color = colors.textPrimary)
                Text("角色：${user?.role ?: "-"}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.neonGreen)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Change password
        StationeryCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showChangePassword = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StationeryIcon(name = "key", size = 18.dp, tint = colors.neonBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("修改密码", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("简化流程，快速重置您的登录密码", fontSize = 13.sp, color = colors.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Configure server address
        StationeryCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showServerConfig = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StationeryIcon(name = "link", size = 18.dp, tint = colors.neonBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("配置服务地址", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("设置并测试内网与外网连接基准", fontSize = 13.sp, color = colors.textSecondary)
                }
            }
        }

        if (user?.role == "STUDENT") {
            Spacer(modifier = Modifier.height(12.dp))

            // 4. Video upload compression (Student only)
            StationeryCard(
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
                    StationeryIcon(name = "video", size = 18.dp, tint = colors.neonBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("视频上传压缩设置", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("当前品质: $activeQuality", fontSize = 13.sp, color = colors.textSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Personalized theme color scheme
        StationeryCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showThemeSettings = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StationeryIcon(name = "palette", size = 18.dp, tint = colors.neonBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val activeThemeName = ThemeList.find { it.id == currentThemeId }?.name ?: "默认"
                    Text("个性化主题配色", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("当前方案: $activeThemeName", fontSize = 13.sp, color = colors.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. System Diagnostics (Always Last!)
        StationeryCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDebugLogs = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StationeryIcon(name = "file", size = 18.dp, tint = colors.neonBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("系统诊断日志", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("查看调试、网络和附件加载状态", fontSize = 13.sp, color = colors.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                showLogoutConfirm = true
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.neonRed)
        ) {
            StationeryIcon(name = "logout", size = 16.dp, tint = colors.neonRed)
            Spacer(modifier = Modifier.width(6.dp))
            Text("退出登录", fontWeight = FontWeight.Bold)
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

