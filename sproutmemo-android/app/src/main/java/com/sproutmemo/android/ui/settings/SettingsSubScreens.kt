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
            Text("诊断调试日志", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                text = if (filteredLogs.isBlank()) "暂无日志数据..." else filteredLogs,
                color = Color(0xFF38BDF8),
                fontSize = 13.sp,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            StationeryIcon(name = "key", size = 20.dp, tint = colors.neonBlue)
            Text("修改密码", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
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
            .gridPaper(colors.darkBg, colors.rule)
            .padding(16.dp)
    ) {
        // Top Row Title only
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            StationeryIcon(name = "palette", size = 20.dp, tint = colors.neonBlue)
            Text("个性化主题配色", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        StationeryCard(
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

                val groupedThemes = remember { ThemeList.groupBy { it.category } }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    groupedThemes.forEach { (category, themesInCat) ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = category,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.neonBlue
                            )
                            themesInCat.forEach { theme ->
                                val isSelected = theme.id == currentThemeId
                                StationeryCard(
                                    containerColor = theme.cardBg,
                                    borderColor = if (isSelected) colors.neonBlue else theme.edge,
                                    cornerRadius = 14.dp,
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
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(theme.darkBg)
                                                    .border(1.dp, theme.edge, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(theme.neonBlue)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = theme.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.textPrimary
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(theme.neonBlue))
                                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(theme.neonGreen))
                                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(theme.neonAmber))
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                StationeryIcon(name = "check", size = 15.dp, tint = colors.neonBlue)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

