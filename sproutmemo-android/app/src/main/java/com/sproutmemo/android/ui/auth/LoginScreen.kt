package com.sproutmemo.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.StationeryPrimaryButton
import com.sproutmemo.android.ui.components.StationeryWashiTape
import com.sproutmemo.android.ui.components.gridPaper
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = remember(configuration) { configuration.screenWidthDp >= 600 }
    val colors = LocalAppColors.current

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
            .gridPaper(colors.darkBg, colors.rule)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = if (isTablet) {
                Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 460.dp)
                    .padding(16.dp)
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            },
            contentAlignment = Alignment.Center
        ) {
            StationeryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                cornerRadius = 22.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = if (isTablet) 28.dp else 20.dp,
                            vertical = if (isTablet) 28.dp else 28.dp
                        )
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StationeryIcon(name = "sprout", size = 28.dp, tint = colors.neonBlue)
                        Text(
                            text = "知拾 SproutMemo",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    Text(
                        text = "错题复习与学习管理系统",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StationeryIcon(name = "settings", size = 15.dp, tint = colors.neonBlue)
                            Text(
                                text = "服务器内外网 URL 配置",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.neonBlue
                            )
                        }
                        Text(
                            text = if (showServerConfig) "收起" else "展开设置",
                            fontSize = 13.5.sp,
                            color = colors.textSecondary
                        )
                    }

                    if (showServerConfig) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.paper2, shape = RoundedCornerShape(12.dp))
                                .border(1.dp, colors.rule, RoundedCornerShape(12.dp))
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
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isDiscovering
                            ) {
                                if (isDiscovering) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("正在扫描局域网 SproutMemo 服务 (mDNS)...", fontSize = 13.5.sp)
                                } else {
                                    StationeryIcon(name = "link", size = 14.dp, tint = colors.neonBlue)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("自动扫描局域网服务 (mDNS)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "说明：登录时优先探测内网；若内网不通，自动切换降级至外网 URL。",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    if (activeServerUrl.isNotBlank()) {
                        Text(
                            text = "当前激活地址: $activeServerUrl",
                            fontSize = 13.sp,
                            color = colors.neonGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (authState is AuthState.Loading) {
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue),
                            enabled = false
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在探测网络并登录...", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    } else {
                        StationeryPrimaryButton(
                            text = "登 录",
                            onClick = {
                                viewModel.login(username, password, intranetInput, extranetInput)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            StationeryWashiTape(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp)
            )
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
                Text(text = "提示信息", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
            },
            text = {
                Text(text = errorMessage ?: "", fontSize = 14.sp, color = colors.textSecondary)
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.cardBg
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
                Text(text = "自动发现成功", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.neonGreen)
            },
            text = {
                Text(text = infoMessage ?: "", fontSize = 14.sp, color = colors.textSecondary)
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.cardBg
        )
    }
}
