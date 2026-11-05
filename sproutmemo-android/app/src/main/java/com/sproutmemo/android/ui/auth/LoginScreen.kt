package com.sproutmemo.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = remember(configuration) { configuration.screenWidthDp >= 600 }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val savedIntranetUrl by viewModel.intranetUrl.collectAsState()
    val savedExtranetUrl by viewModel.extranetUrl.collectAsState()
    val activeServerUrl by viewModel.activeServerUrl.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()

    var intranetInput by remember(savedIntranetUrl) { mutableStateOf(savedIntranetUrl) }
    var extranetInput by remember(savedExtranetUrl) { mutableStateOf(savedExtranetUrl) }
    var showServerConfig by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> onLoginSuccess(state.role)
            is AuthState.Error -> errorMessage = state.message
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4F46E5), Color(0xFF0EA5E9))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = if (isTablet) {
                Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 460.dp)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            },
            shape = if (isTablet) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isTablet) 12.dp else 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = if (isTablet) 28.dp else 20.dp,
                        vertical = if (isTablet) 28.dp else 36.dp
                    )
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌱 知拾 SproutMemo",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "错题复习与学习管理系统",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名 / 账号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Server Configuration Toggle Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showServerConfig = !showServerConfig }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ 服务器内外网 URL 配置",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                    Text(
                        text = if (showServerConfig) "▲ 折叠" else "▼ 展开设置",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (showServerConfig) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        OutlinedTextField(
                            value = intranetInput,
                            onValueChange = { intranetInput = it },
                            label = { Text("内网 URL (LAN)") },
                            placeholder = { Text("例如 http://192.168.1.100:8080") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = extranetInput,
                            onValueChange = { extranetInput = it },
                            label = { Text("外网 URL (WAN)") },
                            placeholder = { Text("例如 https://sproutmemo.zip") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.discoverLanServer(
                                    onDiscovered = { url ->
                                        intranetInput = url
                                        infoMessage = "成功扫描到局域网 SproutMemo 服务器：\n$url"
                                    },
                                    onError = { err ->
                                        errorMessage = err
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isDiscovering
                        ) {
                            if (isDiscovering) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("正在扫描局域网 SproutMemo 服务 (mDNS)...", fontSize = 12.sp)
                            } else {
                                Text("📡 自动扫描局域网服务 (mDNS)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 说明：登录时优先探测内网；若内网不通，自动切换降级至外网 URL。",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                if (activeServerUrl.isNotBlank()) {
                    Text(
                        text = "当前激活地址: $activeServerUrl",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.login(username, password, intranetInput, extranetInput)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    enabled = authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在探测网络并登录...", fontSize = 14.sp)
                        }
                    } else {
                        Text(
                            text = "登 录",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Custom Error Dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("确定", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(text = "提示信息", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(text = errorMessage ?: "", fontSize = 14.sp, color = Color(0xFF334155))
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    // Info Dialog
    if (infoMessage != null) {
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            confirmButton = {
                TextButton(onClick = { infoMessage = null }) {
                    Text("好的", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(text = "自动发现成功", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF10B981))
            },
            text = {
                Text(text = infoMessage ?: "", fontSize = 14.sp, color = Color(0xFF334155))
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}
