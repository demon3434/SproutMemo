package com.sproutmemo.android.ui.dashboard

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
import com.sproutmemo.android.data.remote.model.UserDto
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val loading by viewModel.loading.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val boundStudents by viewModel.boundStudents.collectAsState()
    val selectedStudentId by viewModel.selectedStudentId.collectAsState()
    val statsOverview by viewModel.statsOverview.collectAsState()
    val recentStats by viewModel.recentStats.collectAsState()

    var showChildDropdown by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.darkBg)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🌱 学习复习看板",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = if (userRole == "PARENT") "监控关联孩子的复习进度与掌握情况" else "我的错题库与复习数据分析",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            // Parent bound child selector
            if (userRole == "PARENT" && boundStudents.isNotEmpty()) {
                Box {
                    val currentChild: UserDto = boundStudents.find { student -> student.id == selectedStudentId } ?: boundStudents[0]
                    OutlinedButton(
                        onClick = { showChildDropdown = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.cardBg)
                    ) {
                        Text(
                            text = "👶 ${currentChild.nickname ?: currentChild.username}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.neonBlue
                        )
                        Text(" ▼", fontSize = 10.sp, color = colors.textSecondary)
                    }

                    DropdownMenu(
                        expanded = showChildDropdown,
                        onDismissRequest = { showChildDropdown = false }
                    ) {
                        for (child in boundStudents) {
                            DropdownMenuItem(
                                text = { Text(child.nickname ?: child.username, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    viewModel.selectStudent(child.id)
                                    showChildDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.neonBlue)
            }
        } else {
            ReviewPoolStatsCard(stats = statsOverview)
            MasteryFunnelRingCard(stats = statsOverview)
            SubjectDistributionBarCard(stats = statsOverview)
            RecentReviewStatsTable(rows = recentStats)
        }
    }
}
