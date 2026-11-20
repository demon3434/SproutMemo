package com.sproutmemo.android.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.model.QuestionBookDto
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun BookItemCard(
    book: QuestionBookDto,
    index: Int,
    totalCount: Int,
    onMove: (String) -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current

    StationeryCard(
        containerColor = if (book.is_active) colors.cardBg else colors.paper2.copy(alpha = 0.7f),
        cornerRadius = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (book.is_active) 1f else 0.72f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 彩色书脊
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (book.is_active) colors.neonBlue else colors.edge)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Status dot & Book Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (book.is_active) colors.neonGreen else colors.textSecondary.copy(alpha = 0.4f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = book.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            if (!book.is_active) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(colors.neonRed.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "已停用",
                                        color = colors.neonRed,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Right: Actions (Move arrows, Edit, Toggle, Delete)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onMove("up") },
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "▲",
                            fontSize = 13.sp,
                            color = if (index > 0) colors.neonBlue else colors.textSecondary.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { onMove("down") },
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "▼",
                            fontSize = 13.sp,
                            color = if (index < totalCount - 1) colors.neonBlue else colors.textSecondary.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("编辑", color = colors.neonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onToggle,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (book.is_active) "停用" else "启用",
                            color = if (book.is_active) colors.neonRed else colors.neonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        StationeryIcon(name = "trash", size = 16.dp, tint = colors.neonRed)
                    }
                }
            }
        }
    }
}
