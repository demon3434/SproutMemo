package com.sproutmemo.android.ui.note

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.ui.components.StarRatingStickerBar
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.StationeryWashiTape
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun NoteListItemCard(
    note: NoteDto,
    isSelected: Boolean,
    cardContext: Context,
    onSelect: () -> Unit
) {
    val colors = LocalAppColors.current
    val containerBg = if (isSelected) lerp(colors.cardBg, colors.neonBlue, 0.12f) else colors.cardBg
    val headerBg = if (isSelected) lerp(colors.paper2, colors.neonBlue, 0.14f) else colors.paper2
    val imageAttachments = note.attachments.filter { it.file_type?.equals("IMAGE", ignoreCase = true) == true }

    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        StationeryCard(
            containerColor = containerBg,
            borderColor = if (isSelected) colors.neonBlue else colors.rule,
            edgeColor = if (isSelected) colors.neonBlue else colors.edge,
            cornerRadius = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 顶部淡色索引条：学科 + 学期（左） / 掌握度（右）
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(colors.neonBlue.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = note.subject_name ?: "学科",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.neonBlue
                            )
                        }
                        if (!note.semester_name.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(colors.cardBg, RoundedCornerShape(6.dp))
                                    .border(1.dp, colors.rule, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = note.semester_name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                    val (masteryIcon, masteryText, masteryColor) = when (note.mastery_level) {
                        "CLUELESS" -> Triple("mastery-clueless", "完全不会", colors.neonRed)
                        "PARTIAL" -> Triple("mastery-partial", "一知半解", colors.neonAmber)
                        else -> Triple("mastery-mastered", "熟练掌握", colors.neonGreen)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StationeryIcon(name = masteryIcon, size = 15.dp, tint = masteryColor)
                        Text(
                            text = masteryText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                HorizontalDivider(color = colors.rule)

                // 主体区：左侧封面缩略图（如有） + 右侧来源/星级/正文摘要，紧凑对齐无大块留白
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (imageAttachments.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, colors.rule, RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = ApiClient.getFullAttachmentUrl(cardContext, imageAttachments.first().storage_path),
                                contentDescription = "thumb",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (imageAttachments.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(Color(0xAA1E293B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "+${imageAttachments.size - 1}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                            ) {
                                StationeryIcon(
                                    name = if (note.source_type == "BOOK") "book" else "file",
                                    size = 14.dp,
                                    tint = colors.textSecondary
                                )
                                val sourceMeta = buildString {
                                    append(note.question_source_name ?: note.source_paper_name ?: "自定义来源")
                                    if (!note.unit_name.isNullOrBlank()) append(" · ${note.unit_name}")
                                    if (!note.page_number.isNullOrBlank()) append(" · P.${note.page_number}")
                                }
                                Text(
                                    text = sourceMeta,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            StarRatingStickerBar(
                                stars = note.difficulty_stars.toInt(),
                                onStarsChange = null,
                                starSize = 14.dp,
                                showContainer = false
                            )
                        }

                        Text(
                            text = note.auxiliary_text?.takeIf { it.isNotBlank() } ?: "(暂无文字描述)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (note.auxiliary_text.isNullOrBlank()) colors.textSecondary else colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                }

                // 底部日期与附件计数
                HorizontalDivider(color = colors.rule)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.created_at?.substringBefore('T') ?: "",
                        fontSize = 12.sp,
                        color = colors.textSecondary.copy(alpha = 0.8f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val imagesCount = imageAttachments.size
                        val audiosCount = note.attachments.count { it.file_type?.equals("AUDIO", ignoreCase = true) == true }
                        val videosCount = note.attachments.count { it.file_type?.equals("VIDEO", ignoreCase = true) == true }

                        if (imagesCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                StationeryIcon(name = "image", size = 14.dp, tint = colors.textSecondary)
                                Text("$imagesCount", fontSize = 12.sp, color = colors.textPrimary)
                            }
                        }
                        if (audiosCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                StationeryIcon(name = "music", size = 14.dp, tint = colors.textSecondary)
                                Text("$audiosCount", fontSize = 12.sp, color = colors.textPrimary)
                            }
                        }
                        if (videosCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                StationeryIcon(name = "video", size = 14.dp, tint = colors.textSecondary)
                                Text("$videosCount", fontSize = 12.sp, color = colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
        StationeryWashiTape(
            width = 56.dp,
            height = 13.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-5).dp)
        )
    }
}
