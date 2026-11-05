package com.sproutmemo.android.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.ReviewSessionDto
import com.sproutmemo.android.ui.components.AudioPlayerTile

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewPracticeTabletContent(
    session: ReviewSessionDto,
    currentIndex: Int,
    onSelectNote: (Int) -> Unit,
    selectedOutcome: String,
    onSelectOutcome: (String) -> Unit,
    newMastery: String,
    onMasteryChange: (String) -> Unit,
    newStars: Double,
    onStarsChange: (Double) -> Unit,
    excludeFromReview: Boolean,
    onExcludeChange: (Boolean) -> Unit,
    submitting: Boolean,
    onSubmit: () -> Unit,
    timerText: String,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val currentNote = session.notes.orEmpty().getOrNull(currentIndex) ?: return
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 沉浸式复习答题 (平板模式)", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "⏱️ $timerText",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // 左分栏 (65%): 题目内容与多媒体 (滚动)
            Column(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 试卷来源 & 页码
                val sourceName = currentNote.source_paper_name ?: currentNote.question_source_name ?: "习题笔记"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("📖", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sourceName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.weight(1f)
                    )
                    if (!currentNote.page_number.isNullOrBlank()) {
                        Text(
                            text = "P. ${currentNote.page_number}",
                            fontSize = 11.sp,
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFEEF2FF), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("习题正文", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentNote.auxiliary_text ?: "(无描述)",
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 多媒体附件展示区 (按类别分组展示)
                val images = remember(currentNote.id) {
                    currentNote.attachments.filter { it.file_type?.equals("IMAGE", ignoreCase = true) == true }
                }
                val audios = remember(currentNote.id) {
                    currentNote.attachments.filter { it.file_type?.equals("AUDIO", ignoreCase = true) == true }
                }
                val videos = remember(currentNote.id) {
                    currentNote.attachments.filter { it.file_type?.equals("VIDEO", ignoreCase = true) == true }
                }

                if (images.isNotEmpty() || audios.isNotEmpty() || videos.isNotEmpty()) {
                    Text("多媒体附件", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        images.forEach { attach ->
                            val fullUrl = ApiClient.getFullAttachmentUrl(context, attach.storage_path)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .clickable { onOpenImage(fullUrl) }
                            ) {
                                AsyncImage(
                                    model = fullUrl,
                                    contentDescription = "Attachment Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    text = "🔍 点击放大缩放",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(topStart = 8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        audios.forEach { attach ->
                            val fullUrl = ApiClient.getFullAttachmentUrl(context, attach.storage_path)
                            AudioPlayerTile(
                                audioUrl = fullUrl,
                                title = attach.original_name ?: "音频文件"
                            )
                        }

                        videos.forEach { attach ->
                            val fullUrl = ApiClient.getFullAttachmentUrl(context, attach.storage_path)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray)
                                    .clickable { onOpenVideo(fullUrl) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎬", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = attach.original_name ?: "点击播放视频",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 右分栏 (35%): 进度、面包屑、控制面板 (固定)
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. 进度卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎯 复习进度: ${session.completed_count} / ${session.target_count}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "题号切换:",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 数字瓷砖面包屑
                        FlowRow(
                            maxItemsInEachRow = 5,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            session.notes.orEmpty().forEachIndexed { idx, n ->
                                val isSelected = idx == currentIndex
                                val isCompleted = n.is_completed || (session.completed_count > idx && !isSelected) // or logic
                                
                                Button(
                                    onClick = { onSelectNote(idx) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFF4F46E5) else if (isCompleted) Color(0xFFE8F5E9) else Color(0xFFF1F5F9),
                                        contentColor = if (isSelected) Color.White else if (isCompleted) Color(0xFF2E7D32) else Color(0xFF475569)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(42.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (isCompleted) {
                                            Text(
                                                text = "✓",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFF2E7D32),
                                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 打卡回单反馈卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📝 本题作答结果",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 反馈三选一 (横向并列)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val outcomes = listOf(
                                Triple("WRONG", "🔴 没做对", Color(0xFFEF4444)),
                                Triple("PARTIALLY_CORRECT", "🟡 勉强答对", Color(0xFFF59E0B)),
                                Triple("CORRECT", "🟢 轻松过关", Color(0xFF10B981))
                            )

                            outcomes.forEach { (code, name, colorTheme) ->
                                val isSelected = selectedOutcome == code
                                Button(
                                    onClick = { onSelectOutcome(code) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) colorTheme else Color(0xFFF8FAFC),
                                        contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colorTheme else Color(0xFFE2E8F0))
                                ) {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                             }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(16.dp))

                        // 掌握度修改
                        Text("掌握程度评估", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple("CLUELESS", "未掌握", Color(0xFFEF4444)),
                                Triple("PARTIAL", "略微掌握", Color(0xFFF59E0B)),
                                Triple("MASTERED", "熟练掌握", Color(0xFF10B981))
                            ).forEach { (level, label, color) ->
                                val isSelected = newMastery == level
                                val emoji = when (level) {
                                    "CLUELESS" -> "🔴"
                                    "PARTIAL" -> "🟡"
                                    else -> "🟢"
                                }
                                OutlinedButton(
                                    onClick = { onMasteryChange(level) },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (isSelected) color else Color(0xFF475569)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) color else Color(0xFFE2E8F0)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(emoji, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = label,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 难度星级修改
                        Text("难度星级评估", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (1..5).forEach { star ->
                                Text(
                                    text = "★",
                                    fontSize = 28.sp,
                                    color = if (star <= newStars) Color(0xFFF59E0B) else Color(0xFFcbd5e1),
                                    modifier = Modifier
                                        .clickable { onStarsChange(star.toDouble()) }
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 纳入复习题库
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "纳入复习题库",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Switch(
                                checked = !excludeFromReview,
                                onCheckedChange = { onExcludeChange(!it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF4F46E5),
                                    checkedTrackColor = Color(0xFF4F46E5).copy(alpha = 0.3f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 确认提交与上一题/下一题翻页
                        val isLast = currentIndex >= session.target_count - 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onSelectNote(currentIndex - 1) },
                                enabled = currentIndex > 0,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(44.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text("上一题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            }
                            
                            Button(
                                onClick = onSubmit,
                                enabled = selectedOutcome.isNotEmpty() && !submitting,
                                modifier = Modifier.weight(2f).height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                            ) {
                                if (submitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(
                                        text = if (currentNote.is_completed) "更新打卡" else if (isLast) "提交完成" else "提交切题",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { onSelectNote(currentIndex + 1) },
                                enabled = currentIndex < session.target_count - 1,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(44.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text("下一题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            }
                        }
                    }
                }
            }
        }
    }
}
