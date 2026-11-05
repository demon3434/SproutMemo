package com.sproutmemo.android.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
fun ReviewPracticePhoneContent(
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
    var showBreadcrumbsDialog by remember { mutableStateOf(false) }

    // Breadcrumbs Dialog for phone view
    if (showBreadcrumbsDialog) {
        AlertDialog(
            onDismissRequest = { showBreadcrumbsDialog = false },
            confirmButton = {
                TextButton(onClick = { showBreadcrumbsDialog = false }) { Text("关闭") }
            },
            title = { Text("切换题号", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                FlowRow(
                    maxItemsInEachRow = 5,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    session.notes.orEmpty().forEachIndexed { idx, n ->
                        val isSelected = idx == currentIndex
                        val isCompleted = n.is_completed || (session.completed_count > idx && !isSelected)
                        Button(
                            onClick = {
                                onSelectNote(idx)
                                showBreadcrumbsDialog = false
                            },
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
                                        fontSize = 8.sp,
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
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 答题中 (${currentIndex + 1}/${session.target_count})", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showBreadcrumbsDialog = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Jump Menu", tint = Color(0xFF475569))
                    }
                    Text(
                        text = "⏱️ $timerText",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5),
                        modifier = Modifier.padding(end = 12.dp, start = 4.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // 题干与媒体区域 (占据主要区域，滚动)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                
                // 试卷来源
                val sourceName = currentNote.source_paper_name ?: currentNote.question_source_name ?: "习题笔记"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("📖", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sourceName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.weight(1f)
                    )
                    if (!currentNote.page_number.isNullOrBlank()) {
                        Text(
                            text = "P. ${currentNote.page_number}",
                            fontSize = 10.sp,
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFEEF2FF), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentNote.auxiliary_text ?: "(无描述)",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 多媒体列表 (按类别分组展示)
                val images = remember(currentNote.id) {
                    currentNote.attachments.filter { it.file_type?.equals("IMAGE", ignoreCase = true) == true }
                }
                val audios = remember(currentNote.id) {
                    currentNote.attachments.filter { it.file_type?.equals("AUDIO", ignoreCase = true) == true }
                }
                val videos = remember(currentNote.id) {
                    currentNote.attachments.filter { it.file_type?.equals("VIDEO", ignoreCase = true) == true }
                }

                if (images.isNotEmpty()) {
                    images.forEach { attach ->
                        val fullUrl = ApiClient.getFullAttachmentUrl(context, attach.storage_path)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable { onOpenImage(fullUrl) }
                        ) {
                            AsyncImage(
                                model = fullUrl,
                                contentDescription = "Attachment Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = "🔍 点击无极缩放",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(topStart = 8.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                if (audios.isNotEmpty()) {
                    audios.forEach { attach ->
                        val fullUrl = ApiClient.getFullAttachmentUrl(context, attach.storage_path)
                        AudioPlayerTile(
                            audioUrl = fullUrl,
                            title = attach.original_name ?: "音频文件"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                if (videos.isNotEmpty()) {
                    videos.forEach { attach ->
                        val fullUrl = ApiClient.getFullAttachmentUrl(context, attach.storage_path)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray)
                                .clickable { onOpenVideo(fullUrl) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎬", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = attach.original_name ?: "点击播放视频",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // 底部打卡固定区域 (胶囊面板)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "📝 本题作答结果:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // 三选一胶囊按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    containerColor = if (isSelected) colorTheme else Color(0xFFF1F5F9),
                                    contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // 展开选项 (当选中了结果时，滑出细节调整)
                    if (selectedOutcome.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(10.dp))

                        // 掌握度修改
                        Text("掌握程度评估", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple("CLUELESS", "未掌握", Color(0xFFEF4444)),
                                Triple("PARTIAL", "一知半解", Color(0xFFF59E0B)),
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
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (isSelected) color else Color(0xFF475569)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.2.dp else 1.dp,
                                        color = if (isSelected) color else Color(0xFFE2E8F0)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(emoji, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.width(1.dp))
                                        Text(
                                            text = label,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 难度星级修改
                        Text("难度星级评估", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // 纳入复习题库
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "纳入复习题库",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Switch(
                                checked = !excludeFromReview,
                                onCheckedChange = { onExcludeChange(!it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF4F46E5),
                                    checkedTrackColor = Color(0xFF4F46E5).copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 确定提交按钮与上一题/下一题翻页
                        val isLast = currentIndex >= session.target_count - 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onSelectNote(currentIndex - 1) },
                                enabled = currentIndex > 0,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text("上一题", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            }
                            
                            Button(
                                onClick = onSubmit,
                                enabled = !submitting,
                                modifier = Modifier.weight(2f).height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                            ) {
                                if (submitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
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
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text("下一题", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            }
                        }
                    }
                }
            }
        }
    }
}
