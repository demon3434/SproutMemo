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
    val colors = LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StationeryIcon(name = "target", size = 18.dp, tint = colors.neonBlue)
                        Text("沉浸式复习答题 (平板模式)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                actions = {
                    Text(
                        text = timerText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.neonBlue,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.cardBg)
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .gridPaper(colors.darkBg, colors.rule)
        ) {
            // 左分栏 (65%): 题目内容与多媒体 (滚动)
            StationeryCard(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 试卷来源 & 页码
                    val sourceName = currentNote.source_paper_name ?: currentNote.question_source_name ?: "习题笔记"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.paper2, shape = RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        StationeryIcon(
                            name = if (currentNote.source_type == "BOOK") "book" else "file",
                            size = 15.dp,
                            tint = colors.neonBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sourceName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (!currentNote.page_number.isNullOrBlank()) {
                            Text(
                                text = "P. ${currentNote.page_number}",
                                fontSize = 13.sp,
                                color = colors.neonBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(colors.neonBlue.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("习题正文", fontSize = 13.5.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    StationeryLinedNoteBox(
                        text = currentNote.auxiliary_text,
                        emptyPlaceholder = "(无描述)"
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
                        Text("多媒体附件", fontSize = 13.5.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            images.forEach { attach ->
                                val fullUrl = ApiClient.getFullAttachmentUrl(context, attach.storage_path)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.paper2)
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
                                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(topStart = 8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        StationeryIcon(name = "search", size = 12.dp, tint = Color.White)
                                        Text(
                                            text = "点击放大缩放",
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
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
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.paper2)
                                        .border(1.5.dp, colors.edge, RoundedCornerShape(12.dp))
                                        .clickable { onOpenVideo(fullUrl) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        StationeryIcon(name = "video", size = 36.dp, tint = colors.neonBlue)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = attach.original_name ?: "点击播放视频",
                                            color = colors.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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
                StationeryCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StationeryIcon(name = "target", size = 16.dp, tint = colors.neonBlue)
                            Text(
                                text = "复习进度: ${session.completed_count} / ${session.target_count}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        StationeryCandyProgressBar(
                            progress = session.completed_count.toFloat() / session.target_count.coerceAtLeast(1)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "题号切换:",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
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
                                val isCompleted = n.is_completed || (session.completed_count > idx && !isSelected)
                                
                                Button(
                                    onClick = { onSelectNote(idx) },
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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 打卡回单反馈卡片
                StationeryCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StationeryIcon(name = "pen", size = 16.dp, tint = colors.neonBlue)
                            Text(
                                text = "本题作答结果",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // 反馈三选一 (立体卡纸按钮)
                        ReviewOutcomeRow(
                            selectedOutcome = selectedOutcome,
                            onSelect = onSelectOutcome
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = colors.rule)
                        Spacer(modifier = Modifier.height(16.dp))

                        // 掌握度修改
                        Text("掌握程度评估", fontSize = 13.5.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        MasteryStampRow(
                            selectedLevel = newMastery,
                            onSelect = onMasteryChange
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 难度星级修改
                        Text("难度星级评估", fontSize = 13.5.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        StarRatingStickerBar(
                            stars = newStars.toInt(),
                            onStarsChange = { onStarsChange(it.toDouble()) },
                            starSize = 26.dp
                        )

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
                                color = colors.textPrimary
                            )
                            Switch(
                                checked = !excludeFromReview,
                                onCheckedChange = { onExcludeChange(!it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.neonBlue,
                                    checkedTrackColor = colors.neonBlue.copy(alpha = 0.3f)
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.edge)
                            ) {
                                Text("上一题", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            }
                            
                            Button(
                                onClick = onSubmit,
                                enabled = selectedOutcome.isNotEmpty() && !submitting,
                                modifier = Modifier.weight(2f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                            ) {
                                if (submitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
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
                                modifier = Modifier.weight(1f).height(44.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.edge)
                            ) {
                                Text("下一题", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
