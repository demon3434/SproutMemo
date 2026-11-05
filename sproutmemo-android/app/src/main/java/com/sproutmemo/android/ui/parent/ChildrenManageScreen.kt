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

@Composable
fun ChildrenManageScreen(viewModel: ChildrenManageViewModel) {
    val children by viewModel.children.collectAsState()
    val loading by viewModel.loading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Text("👶 我的孩子账号管理", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Text("可为此账号下的关联孩子调整学期偏移量", fontSize = 12.sp, color = Color(0xFF64748B))

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4F46E5))
            }
        } else if (children.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("👶 暂未关联任何孩子账号", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(children) { child ->
                    val offset = child.semester_offset ?: 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                            .background(Color(0xFF4F46E5), shape = CircleShape),
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
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "账号: ${child.username} · 入学年份: ${child.enrollment_year ?: "未设置"}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                AssistChip(
                                    onClick = {},
                                    label = { Text(if (child.is_active) "正常" else "停用", fontSize = 11.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("学期偏移量调整", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
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
                                        .background(Color(0xFFEEF2FF), shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${if (offset > 0) "+" else ""}$offset 学期",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
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
                            Text(
                                text = "📅 当前推算学期：${viewModel.calculateSemesterName(child.enrollment_year, offset)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }
    }
}
