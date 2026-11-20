package com.sproutmemo.android.ui.review

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.components.RecentStatsTable
import com.sproutmemo.android.ui.components.ReviewPresetsStatsCard
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.gridPaper
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun DailyReviewScreen(
    viewModel: ReviewViewModel,
    userRole: String
) {
    val colors = LocalAppColors.current
    val presetsData by viewModel.presetsData.collectAsState()
    val recentStats by viewModel.recentStats.collectAsState()
    val boundChildren by viewModel.boundChildren.collectAsState()
    val selectedChildId by viewModel.selectedChildId.collectAsState()

    val isParent = userRole == "PARENT"

    // Load initial metadata and statistics
    LaunchedEffect(userRole, selectedChildId) {
        if (isParent) {
            viewModel.loadBoundChildren(userRole)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .gridPaper(colors.darkBg, colors.rule)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StationeryIcon(name = "chart", size = 22.dp, tint = colors.neonBlue)
            Text("复习总览仪表盘", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (isParent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Child Selector Card
                if (boundChildren.isNotEmpty()) {
                    var childSelectorExpanded by remember { mutableStateOf(false) }
                    val activeChild = boundChildren.find { it.id == selectedChildId }
                    val childDisplayName = activeChild?.nickname ?: activeChild?.username ?: "选择孩子账号"

                    StationeryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("选择学生账号:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            Box {
                                OutlinedButton(
                                    onClick = { childSelectorExpanded = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(childDisplayName, color = colors.neonBlue, fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(
                                    expanded = childSelectorExpanded,
                                    onDismissRequest = { childSelectorExpanded = false },
                                    modifier = Modifier.background(colors.cardBg)
                                ) {
                                    boundChildren.forEach { child ->
                                        DropdownMenuItem(
                                            text = { Text(child.nickname ?: child.username, color = colors.textPrimary) },
                                            onClick = {
                                                viewModel.onChildSelected(child.id)
                                                childSelectorExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ReviewPresetsStatsCard(stats = presetsData?.stats)
                RecentStatsTable(recentStats = recentStats)
            }
        } else {
            // Student placeholder (should be directed to DrawReview in navigation)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("此面板仅家长端账号可见，学生账号请使用“抽题复习”。", color = colors.textSecondary, fontSize = 14.sp)
            }
        }
    }
}
