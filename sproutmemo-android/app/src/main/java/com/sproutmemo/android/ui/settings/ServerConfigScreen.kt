package com.sproutmemo.android.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.local.TokenManager
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.NetworkProbe
import com.sproutmemo.android.data.remote.NsdHelper
import com.sproutmemo.android.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { ApiClient.getTokenManager(context) }
    val colors = LocalAppColors.current

    var intranetUrl by remember { mutableStateOf("") }
    var extranetUrl by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        intranetUrl = tokenManager.getIntranetUrlSync()
        extranetUrl = tokenManager.getExtranetUrlSync()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.darkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("⚙️ 配置服务地址", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "请在此配置知拾服务端的连通地址。系统将优先尝试内网，连接失败时自动尝试备用外网。",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                    value = intranetUrl,
                    onValueChange = { intranetUrl = it },
                    label = { Text("内网服务地址 (LAN URL)") },
                    placeholder = { Text("例如 http://192.168.1.100:8080") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isDiscovering = true
                            testResult = "⏳ 正在扫描局域网 SproutMemo 服务 (mDNS)..."
                            try {
                                val found = NsdHelper.discoverServer(context, timeoutMs = 5000)
                                if (!found.isNullOrBlank()) {
                                    intranetUrl = found
                                    testResult = "✅ 成功扫描到局域网服务：$found (已填入文本框)"
                                    Toast.makeText(context, "📡 成功扫描到内网服务并已填入", Toast.LENGTH_SHORT).show()
                                } else {
                                    testResult = "❌ 未在局域网内扫描到 SproutMemo 服务 (mDNS)，请确认手机与服务端处于同一 Wi-Fi 网络"
                                    Toast.makeText(context, "未扫描到局域网服务", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                testResult = "❌ mDNS 扫描异常: ${e.localizedMessage}"
                            } finally {
                                isDiscovering = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.neonBlue),
                    enabled = !isDiscovering && !testing
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.neonBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在扫描局域网 SproutMemo 服务 (mDNS)...", fontSize = 12.sp)
                    } else {
                        Text("📡 内网自动发现 (mDNS)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = extranetUrl,
                    onValueChange = { extranetUrl = it },
                    label = { Text("外网服务地址 (WAN URL)") },
                    placeholder = { Text("例如 https://sproutmemo.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors
                )

                if (testResult.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = testResult,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (testResult.contains("✅")) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (intranetUrl.isBlank() && extranetUrl.isBlank()) {
                                testResult = "⚠️ 请输入至少一个服务地址"
                                return@OutlinedButton
                            }
                            testing = true
                            testResult = "⏳ 正在测试连通性..."
                            scope.launch {
                                val intraOk = if (intranetUrl.isNotBlank()) NetworkProbe.probeUrl(intranetUrl) else false
                                val extraOk = if (extranetUrl.isNotBlank()) NetworkProbe.probeUrl(extranetUrl) else false
                                testing = false
                                
                                val sb = java.lang.StringBuilder()
                                if (intranetUrl.isNotBlank()) {
                                    sb.append("内网: ").append(if (intraOk) "✅ 连通成功" else "❌ 连通失败")
                                }
                                if (extranetUrl.isNotBlank()) {
                                    if (sb.isNotEmpty()) sb.append("\n")
                                    sb.append("外网: ").append(if (extraOk) "✅ 连通成功" else "❌ 连通失败")
                                }
                                testResult = sb.toString()
                            }
                        },
                        enabled = !testing && !isDiscovering,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.neonBlue)
                    ) {
                        Text("测试连通性", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val active = if (intranetUrl.isNotBlank()) intranetUrl else extranetUrl
                                tokenManager.saveServerUrls(intranetUrl, extranetUrl, active)
                                com.sproutmemo.android.data.remote.NetworkProbeManager.probeAndSelectBestUrl(context)
                                Toast.makeText(context, "💾 配置已保存", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        enabled = !testing && !isDiscovering,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                    ) {
                        Text("保存配置", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
