package com.sproutmemo.android.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.sproutmemo.android.ui.components.MasteryStampRow
import com.sproutmemo.android.ui.components.ReviewOutcomeRow
import com.sproutmemo.android.ui.components.StarRatingStickerBar
import com.sproutmemo.android.ui.components.StationeryCandyProgressBar
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.StationeryLinedNoteBox
import com.sproutmemo.android.ui.components.gridPaper
import com.sproutmemo.android.ui.theme.LocalAppColors

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
    val colors = LocalAppColors.current
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
                                containerColor = if (isSelected) colors.neonBlue else if (isCompleted) colors.neonGreen.copy(alpha = 0.16f) else colors.paper2,
                                contentColor = if (isSelected) Color.White else if (isCompleted) colors.neonGreen else colors.textSecondary
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
                                    StationeryIcon(
                                        name = "check",
                                        size = 9.dp,
                                        tint = if (isSelected) Color.White else colors.neonGreen,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = colors.cardBg
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StationeryIcon(name = "target", size = 18.dp, tint = colors.neonBlue)
                        Text("答题中 (${currentIndex + 1}/${session.target_count})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showBreadcrumbsDialog = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Jump Menu", tint = colors.textSecondary)
                    }
                    Text(
                        text = timerText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.neonBlue,
                        modifier = Modifier.padding(end = 12.dp, start = 4.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.cardBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .gridPaper(colors.darkBg, colors.rule)
        ) {
            StationeryCandyProgressBar(
                progress = session.completed_count.toFloat() / session.target_count.coerceAtLeast(1),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
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
                        .background(colors.paper2, shape = RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    StationeryIcon(
                        name = if (currentNote.source_type == "BOOK") "book" else "file",
                        size = 14.dp,
                        tint = colors.neonBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sourceName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (!currentNote.page_number.isNullOrBlank()) {
                        Text(
                            text = "P. ${currentNote.page_number}",
                            fontSize = 13.5.sp,
                            color = colors.neonBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(colors.neonBlue.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                StationeryLinedNoteBox(
                    text = currentNote.auxiliary_text,
                    emptyPlaceholder = "(无描述)"
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(topStart = 8.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                StationeryIcon(name = "search", size = 11.dp, tint = Color.White)
                                Text(
                                    text = "点击无极缩放",
                                    color = Color.White,
                                    fontSize = 13.5.sp
                                )
                            }
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.paper2)
                                .border(1.5.dp, colors.edge, RoundedCornerShape(12.dp))
                                .clickable { onOpenVideo(fullUrl) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                StationeryIcon(name = "video", size = 32.dp, tint = colors.neonBlue)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = attach.original_name ?: "点击播放视频",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // 底部打卡固定区域 (胶囊面板)
            StationeryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        StationeryIcon(name = "pen", size = 13.dp, tint = colors.textSecondary)
                        Text(
                            text = "本题作答结果:",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                    }

                    // 三选一立体卡纸按钮
                    ReviewOutcomeRow(
                        selectedOutcome = selectedOutcome,
                        onSelect = onSelectOutcome
                    )

                    // 展开选项 (当选中了结果时，滑出细节调整)
                    if (selectedOutcome.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = colors.rule)
                        Spacer(modifier = Modifier.height(10.dp))

                        // 掌握度修改
                        Text("掌握程度评估", fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        MasteryStampRow(
                            selectedLevel = newMastery,
                            onSelect = onMasteryChange
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 难度星级修改
                        Text("难度星级评估", fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        StarRatingStickerBar(
                            stars = newStars.toInt(),
                            onStarsChange = { onStarsChange(it.toDouble()) },
                            starSize = 26.dp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 纳入复习题库
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "纳入复习题库",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Switch(
                                checked = !excludeFromReview,
                                onCheckedChange = { onExcludeChange(!it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.neonBlue,
                                    checkedTrackColor = colors.neonBlue.copy(alpha = 0.3f)
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.edge)
                            ) {
                                Text("上一题", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            }
                            
                            Button(
                                onClick = onSubmit,
                                enabled = !submitting,
                                modifier = Modifier.weight(2f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                            ) {
                                if (submitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text(
                                        text = if (currentNote.is_completed) "更新打卡" else if (isLast) "提交完成" else "提交切题",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { onSelectNote(currentIndex + 1) },
                                enabled = currentIndex < session.target_count - 1,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.edge)
                            ) {
                                Text("下一题", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
