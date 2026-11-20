package com.sproutmemo.android.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.sproutmemo.android.R
import com.sproutmemo.android.data.remote.NetworkProbeManager
import com.sproutmemo.android.data.remote.ConnectionStatus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.CloudOff
import com.sproutmemo.android.data.local.TokenManager
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.gridPaper
import com.sproutmemo.android.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: String, val parentOnly: Boolean = false, val studentOnly: Boolean = false) {
    object Dashboard : Screen("dashboard", "首页", "chart")
    object NoteCreate : Screen("note_create", "录入习题", "pen", studentOnly = true)
    object NoteList : Screen("note_list", "查看习题", "list")
    object NoteEdit : Screen("note_edit/{noteId}", "编辑习题", "pen", studentOnly = true)
    object DailyReview : Screen("daily_review", "我要复习", "target", parentOnly = true)
    object BookManage : Screen("book_manage", "我的习题册", "book")
    object ChildrenManage : Screen("children_manage", "我的孩子", "child", parentOnly = true)
    object Settings : Screen("settings", "设置", "settings")
    object ReviewPractice : Screen("review_practice/{sessionId}", "复习答题", "pen", studentOnly = true)
    object DrawReview : Screen("draw_review", "抽题复习", "target", studentOnly = true)
    object ReviewHistory : Screen("review_history", "复习记录", "list")
    object NotePreview : Screen("note_preview/{noteId}", "题目预览", "search")
}

@Composable
fun AppShell(
    userRole: String,
    currentScreen: Screen,
    onScreenSelect: (Screen) -> Unit,
    onLogout: () -> Unit,
    isTabletLandscape: Boolean = false,
    onBackPressed: (() -> Unit)? = null,
    onNavigateToHistory: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    if (currentScreen.route.startsWith("review_practice") || currentScreen.route.startsWith("note_preview")) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.darkBg)
        ) {
            content()
        }
        return
    }

    val navScreens = remember(userRole) {
        listOf(
            Screen.Dashboard,
            Screen.NoteCreate,
            Screen.NoteList,
            // Exclude DailyReview, DrawReview, ReviewHistory from main flat list
            Screen.BookManage,
            Screen.ChildrenManage,
            Screen.Settings
        ).filter { screen ->
            if (userRole == "PARENT" && screen.studentOnly) false
            else if (userRole == "STUDENT" && screen.parentOnly) false
            else true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.paper2,
                modifier = Modifier
                    .width(276.dp)
                    .fillMaxHeight()
                    .border(1.5.dp, colors.rule)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Header Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, colors.edge, RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "知拾",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "SproutMemo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        HorizontalDivider(color = colors.rule, thickness = 1.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "我的错题本",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )

                        // 1. Top Screens (Dashboard, NoteCreate, NoteList)
                        navScreens.filter { it == Screen.Dashboard || it == Screen.NoteCreate || it == Screen.NoteList }.forEach { screen ->
                            StationeryDrawerItem(
                                iconName = screen.icon,
                                label = screen.title,
                                isSelected = currentScreen.route == screen.route,
                                onClick = {
                                    onScreenSelect(screen)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }

                        // 2. Single Review flat item
                        val isReviewSelected = currentScreen.route.startsWith("draw_review") ||
                                              currentScreen.route.startsWith("daily_review") ||
                                              currentScreen.route.startsWith("review_history") ||
                                              currentScreen.route.startsWith("note_preview")
                        if (userRole == "STUDENT") {
                            StationeryDrawerItem(
                                iconName = "target",
                                label = "我要复习",
                                isSelected = isReviewSelected,
                                onClick = {
                                    val targetScreen = if (userRole == "PARENT") Screen.DailyReview else Screen.DrawReview
                                    onScreenSelect(targetScreen)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = colors.rule)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "习题册配置",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )

                        // 3. Bottom Screens (BookManage, ChildrenManage)
                        navScreens.filter { it != Screen.Dashboard && it != Screen.NoteCreate && it != Screen.NoteList && it != Screen.Settings }.forEach { screen ->
                            val displayTitle = if (screen == Screen.BookManage && userRole == "PARENT") "孩子的习题册" else screen.title
                            StationeryDrawerItem(
                                iconName = screen.icon,
                                label = displayTitle,
                                isSelected = currentScreen.route == screen.route,
                                onClick = {
                                    onScreenSelect(screen)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }

                    // Footer / Settings
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp)
                    ) {
                        HorizontalDivider(color = colors.rule)
                        Spacer(modifier = Modifier.height(8.dp))
                        StationeryDrawerItem(
                            iconName = Screen.Settings.icon,
                            label = Screen.Settings.title,
                            isSelected = currentScreen.route == Screen.Settings.route,
                            onClick = {
                                onScreenSelect(Screen.Settings)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .gridPaper(colors.darkBg, colors.rule.copy(alpha = 0.35f))
                .navigationBarsPadding()
        ) {
            // Unified Stationery Top Bar
            Surface(
                color = colors.cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    if (onBackPressed != null) {
                        IconButton(onClick = onBackPressed) {
                            StationeryIcon(name = "chevron-left", size = 22.dp, tint = colors.textPrimary)
                        }
                    } else {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "菜单",
                                tint = colors.textPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    StationeryIcon(name = currentScreen.icon, size = 18.dp, tint = colors.neonBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentScreen == Screen.NoteCreate) "录入新习题"
                        else if (currentScreen == Screen.NoteEdit) "编辑习题"
                        else currentScreen.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                // Push all actions to the right
                Spacer(modifier = Modifier.weight(1f))

                val context = androidx.compose.ui.platform.LocalContext.current
                val connectionStatus by NetworkProbeManager.status.collectAsState()
                val isProbingRoute by NetworkProbeManager.isProbing.collectAsState()
                val probeStatusMessage by NetworkProbeManager.probeStatusMessage.collectAsState()

                val tokenManager = remember { TokenManager(context) }
                val activeServerUrl by tokenManager.activeServerUrlFlow.collectAsState(initial = "")

                val netState = when (connectionStatus) {
                    ConnectionStatus.INTRANET -> {
                        Pair(Icons.Default.Router, Color(0xFF10B981))
                    }
                    ConnectionStatus.EXTRANET -> {
                        Pair(Icons.Default.Public, Color(0xFF3B82F6))
                    }
                    ConnectionStatus.OFFLINE -> {
                        Pair(Icons.Default.CloudOff, Color.Gray)
                    }
                }

                if (currentScreen == Screen.DrawReview || currentScreen == Screen.DailyReview) {
                    IconButton(onClick = { onNavigateToHistory?.invoke() ?: onScreenSelect(Screen.ReviewHistory) }) {
                        StationeryIcon(name = "list", size = 20.dp, tint = colors.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                var isNetworkInfoExpanded by remember { mutableStateOf(false) }

                Box {
                    IconButton(onClick = {
                        isNetworkInfoExpanded = true
                        NetworkProbeManager.trySwitchToIntranet(context)
                    }) {
                        if (isProbingRoute) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF10B981)
                            )
                        } else {
                            Icon(
                                imageVector = netState.first,
                                contentDescription = "Network State Indicator",
                                tint = netState.second,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isNetworkInfoExpanded,
                        onDismissRequest = { isNetworkInfoExpanded = false },
                        modifier = Modifier
                            .background(colors.cardBg)
                            .padding(12.dp)
                            .width(240.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StationeryIcon(name = "link", size = 15.dp, tint = colors.neonBlue)
                                Text(
                                    text = "同步通道详情",
                                    color = colors.neonBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            HorizontalDivider(color = colors.rule)

                            val routeTitle = when (connectionStatus) {
                                ConnectionStatus.INTRANET -> "家庭局域网 (内网)"
                                ConnectionStatus.EXTRANET -> "云端服务线路 (外网)"
                                ConnectionStatus.OFFLINE -> "网络连接异常 (离线)"
                            }
                            Text(
                                text = "当前线路: $routeTitle",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "物理地址:\n${activeServerUrl.ifBlank { "未连接" }}",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                lineHeight = 15.sp
                            )

                            if (connectionStatus == ConnectionStatus.INTRANET) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    StationeryIcon(name = "sprout", size = 13.dp, tint = colors.neonGreen)
                                    Text(
                                        text = "正在享受局域网极速同步",
                                        color = colors.neonGreen,
                                        fontSize = 13.sp
                                    )
                                }
                            } else if (connectionStatus == ConnectionStatus.EXTRANET) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    StationeryIcon(name = "link", size = 13.dp, tint = colors.neonBlue)
                                    Text(
                                        text = "已切为外网云端兜底线路",
                                        color = colors.neonBlue,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (isProbingRoute) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.6.dp,
                                        color = colors.neonGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "正在探测家庭内网通道...",
                                        color = colors.neonGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (!probeStatusMessage.isNullOrBlank()) {
                                Text(
                                    text = probeStatusMessage ?: "",
                                    color = if (probeStatusMessage?.contains("内网") == true) colors.neonGreen else colors.neonBlue,
                                    fontSize = 13.sp,
                                    lineHeight = 15.sp
                                )
                            }

                            HorizontalDivider(color = colors.rule)

                            Button(
                                onClick = {
                                    NetworkProbeManager.trySwitchToIntranet(context) { success ->
                                        val statusText = if (success) "已切换至家庭局域网 (内网)" else "未检测到家庭内网服务"
                                        android.widget.Toast.makeText(context, statusText, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isProbingRoute,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (isProbingRoute) "正在检测..." else "尝试连接内网通道",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                }
                HorizontalDivider(color = colors.rule, thickness = 1.5.dp)
            }
        }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

