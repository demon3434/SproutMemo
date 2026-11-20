package com.sproutmemo.android.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.gridPaper
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun ChildrenManageScreen(viewModel: ChildrenManageViewModel) {
    val colors = LocalAppColors.current
    val children by viewModel.children.collectAsState()
    val loading by viewModel.loading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .gridPaper(colors.darkBg, colors.rule)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StationeryIcon(name = "child", size = 22.dp, tint = colors.neonBlue)
            Text("我的孩子账号管理", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
        Text("可为此账号下的关联孩子调整学期偏移量", fontSize = 13.5.sp, color = colors.textSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.neonBlue)
            }
        } else if (children.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StationeryIcon(name = "child", size = 18.dp, tint = colors.textSecondary)
                    Text("暂未关联任何孩子账号", color = colors.textSecondary)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(children) { child ->
                    val offset = child.semester_offset ?: 0
                    StationeryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(colors.neonBlue, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (child.nickname ?: child.username).take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = child.nickname ?: child.username,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "账号: ${child.username} · 入学年份: ${child.enrollment_year ?: "未设置"}",
                                            fontSize = 13.5.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }

                                AssistChip(
                                    onClick = {},
                                    label = { Text(if (child.is_active) "正常" else "停用", fontSize = 13.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = colors.rule)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("学期偏移量调整", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.changeSemesterOffset(child.id, offset, -1) },
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("－", fontWeight = FontWeight.Bold) }

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .background(colors.neonBlue.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${if (offset > 0) "+" else ""}$offset 学期",
                                        fontWeight = FontWeight.Bold,
                                        color = colors.neonBlue
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.changeSemesterOffset(child.id, offset, 1) },
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("＋", fontWeight = FontWeight.Bold) }

                                if (offset != 0) {
                                    TextButton(onClick = { viewModel.changeSemesterOffset(child.id, offset, -offset) }) {
                                        Text("重置")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                StationeryIcon(name = "calendar", size = 14.dp, tint = colors.neonGreen)
                                Text(
                                    text = "当前推算学期：${viewModel.calculateSemesterName(child.enrollment_year, offset)}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.neonGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
