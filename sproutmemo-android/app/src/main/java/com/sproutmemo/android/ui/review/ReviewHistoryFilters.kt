package com.sproutmemo.android.ui.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.data.remote.model.ReviewSessionDto
import com.sproutmemo.android.data.remote.model.UserDto
import com.sproutmemo.android.data.remote.model.SubjectDto
import com.sproutmemo.android.ui.components.StarRatingStickerBar
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.gridPaper
import com.sproutmemo.android.ui.theme.LocalAppColors


internal fun formatYearMonth(ym: String?): String {
    if (ym.isNullOrBlank()) return "全部月份"
    val parts = ym.split("-")
    if (parts.size == 2) {
        return "${parts[0]}年${parts[1]}月"
    }
    return ym
}

internal fun changeMonth(ym: String, delta: Int): String {
    val parts = ym.split("-")
    if (parts.size == 2) {
        var y = parts[0].toIntOrNull() ?: 2026
        var m = parts[1].toIntOrNull() ?: 8
        m += delta
        if (m > 12) {
            y += 1
            m = 1
        } else if (m < 1) {
            y -= 1
            m = 12
        }
        return "%d-%02d".format(y, m)
    }
    return ym
}

internal fun getCurrentYearMonth(): String {
    val calendar = java.util.Calendar.getInstance()
    return "%d-%02d".format(calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1)
}

@Composable
internal fun HistoryFilters(
    viewModel: ReviewViewModel,
    historyYearMonth: String?,
    allSubjects: List<SubjectDto>,
    historySubjectId: String?,
    monthList: List<String>
) {
    val colors = LocalAppColors.current
    var monthExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Year/Month Selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(colors.cardBg, shape = RoundedCornerShape(10.dp))
                .border(1.dp, colors.edge, shape = RoundedCornerShape(10.dp))
        ) {
            IconButton(
                onClick = {
                    val current = historyYearMonth ?: getCurrentYearMonth()
                    viewModel.historyYearMonth.value = changeMonth(current, -1)
                },
                enabled = historyYearMonth != null,
                modifier = Modifier.size(36.dp)
            ) {
                StationeryIcon(
                    name = "chevron-left",
                    size = 15.dp,
                    tint = if (historyYearMonth != null) colors.neonBlue else colors.textSecondary.copy(alpha = 0.4f)
                )
            }

            Box {
                TextButton(
                    onClick = { monthExpanded = true },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        StationeryIcon(name = "calendar", size = 14.dp, tint = colors.neonBlue)
                        Text(
                            text = formatYearMonth(historyYearMonth),
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                DropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false },
                    modifier = Modifier.background(colors.cardBg)
                ) {
                    DropdownMenuItem(
                        text = { Text("全部月份", color = colors.textPrimary) },
                        onClick = {
                            viewModel.historyYearMonth.value = null
                            monthExpanded = false
                        }
                    )
                    monthList.forEach { ym ->
                        DropdownMenuItem(
                            text = { Text(formatYearMonth(ym), color = colors.textPrimary) },
                            onClick = {
                                viewModel.historyYearMonth.value = ym
                                monthExpanded = false
                            }
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    val current = historyYearMonth ?: getCurrentYearMonth()
                    viewModel.historyYearMonth.value = changeMonth(current, 1)
                },
                enabled = historyYearMonth != null,
                modifier = Modifier.size(36.dp)
            ) {
                StationeryIcon(
                    name = "chevron-right",
                    size = 15.dp,
                    tint = if (historyYearMonth != null) colors.neonBlue else colors.textSecondary.copy(alpha = 0.4f)
                )
            }
        }

        // Subject Selector
        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .background(colors.cardBg, shape = RoundedCornerShape(10.dp))
                .border(1.dp, colors.edge, shape = RoundedCornerShape(10.dp))
                .clickable { subjectExpanded = true },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedSubject = allSubjects.find { it.subject_id == historySubjectId }
                Text(
                    text = selectedSubject?.subject_name ?: "全部学科",
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text("▼", fontSize = 13.5.sp, color = Color(0xFF64748B))
            }

            DropdownMenu(
                expanded = subjectExpanded,
                onDismissRequest = { subjectExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("全部学科") },
                    onClick = {
                        viewModel.historySubjectId.value = null
                        subjectExpanded = false
                    }
                )
                allSubjects.forEach { sub ->
                    DropdownMenuItem(
                        text = { Text(sub.subject_name) },
                        onClick = {
                            viewModel.historySubjectId.value = sub.subject_id
                            subjectExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ChildSelector(
    viewModel: ReviewViewModel,
    isParent: Boolean,
    boundChildren: List<UserDto>,
    selectedChildId: String?
) {
    if (isParent && boundChildren.isNotEmpty()) {
        var childSelectorExpanded by remember { mutableStateOf(false) }
        val activeChild = boundChildren.find { it.id == selectedChildId }
        val childDisplayName = activeChild?.nickname ?: activeChild?.username ?: "选择孩子账号"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("选择学生账号:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Box {
                    OutlinedButton(
                        onClick = { childSelectorExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(childDisplayName, color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    DropdownMenu(
                        expanded = childSelectorExpanded,
                        onDismissRequest = { childSelectorExpanded = false }
                    ) {
                        boundChildren.forEach { child ->
                            DropdownMenuItem(
                                text = { Text(child.nickname ?: child.username) },
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
}

