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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.data.remote.model.ReviewSessionDto
import com.sproutmemo.android.data.remote.model.UserDto
import com.sproutmemo.android.data.remote.model.SubjectDto

private fun formatYearMonth(ym: String?): String {
    if (ym.isNullOrBlank()) return "全部月份"
    val parts = ym.split("-")
    if (parts.size == 2) {
        return "${parts[0]}年${parts[1]}月"
    }
    return ym
}

private fun changeMonth(ym: String, delta: Int): String {
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

private fun getCurrentYearMonth(): String {
    val calendar = java.util.Calendar.getInstance()
    return "%d-%02d".format(calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1)
}

@Composable
private fun HistoryFilters(
    viewModel: ReviewViewModel,
    historyYearMonth: String?,
    allSubjects: List<SubjectDto>,
    historySubjectId: String?,
    monthList: List<String>
) {
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
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
        ) {
            IconButton(
                onClick = {
                    val current = historyYearMonth ?: getCurrentYearMonth()
                    viewModel.historyYearMonth.value = changeMonth(current, -1)
                },
                enabled = historyYearMonth != null,
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    "◀",
                    fontSize = 11.sp,
                    color = if (historyYearMonth != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                )
            }

            Box {
                TextButton(
                    onClick = { monthExpanded = true },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "📅 ${formatYearMonth(historyYearMonth)}",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                DropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("全部月份") },
                        onClick = {
                            viewModel.historyYearMonth.value = null
                            monthExpanded = false
                        }
                    )
                    monthList.forEach { ym ->
                        DropdownMenuItem(
                            text = { Text(formatYearMonth(ym)) },
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
                Text(
                    "▶",
                    fontSize = 11.sp,
                    color = if (historyYearMonth != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                )
            }
        }

        // Subject Selector
        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
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
                Text("▼", fontSize = 10.sp, color = Color(0xFF64748B))
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
private fun ChildSelector(
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

@Composable
fun ReviewHistoryScreen(
    viewModel: ReviewViewModel,
    userRole: String,
    isTabletLandscape: Boolean,
    onNavigateToNote: (String) -> Unit
) {
    val context = LocalContext.current
    val apiService = remember { ApiClient.getService(context) }

    val historySessions by viewModel.historySessions.collectAsState()
    val allSubjects by viewModel.allSubjects.collectAsState()
    val historyYearMonth by viewModel.historyYearMonth.collectAsState()
    val historySubjectId by viewModel.historySubjectId.collectAsState()
    val boundChildren by viewModel.boundChildren.collectAsState()
    val selectedChildId by viewModel.selectedChildId.collectAsState()

    val isParent = userRole == "PARENT"

    // Load bound children or history when screen opens
    LaunchedEffect(userRole, selectedChildId, historyYearMonth, historySubjectId) {
        if (isParent) {
            viewModel.loadBoundChildren(userRole)
        }
        viewModel.loadReviewHistory()
    }

    // Selected Session state for Tablet Right pane
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var detailedSession by remember { mutableStateOf<ReviewSessionDto?>(null) }
    var loadingDetails by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSessionId) {
        if (selectedSessionId != null) {
            loadingDetails = true
            try {
                val listSession = historySessions.find { it.id == selectedSessionId }
                val res = apiService.getReviewSessionDetails(selectedSessionId!!)
                if (res.isSuccessful && res.body()?.code == 200) {
                    detailedSession = res.body()?.data?.copy(
                        semester_name = listSession?.semester_name,
                        subject_name = listSession?.subject_name
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadingDetails = false
            }
        } else {
            detailedSession = null
        }
    }

    // Helper data for month selector
    val monthList = remember {
        val list = mutableListOf<String>()
        val calendar = java.util.Calendar.getInstance()
        for (i in 0 until 24) {
            val y = calendar.get(java.util.Calendar.YEAR)
            val m = calendar.get(java.util.Calendar.MONTH) + 1
            list.add("%d-%02d".format(y, m))
            calendar.add(java.util.Calendar.MONTH, -1)
        }
        list
    }

    if (isTabletLandscape) {
        // --- Tablet Dual Column Layout ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(16.dp)
        ) {
            // Left Pane (40%): Filters and Sessions List
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(end = 8.dp)
            ) {
                Text("📋 往期复习任务", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(12.dp))
                ChildSelector(
                    viewModel = viewModel,
                    isParent = isParent,
                    boundChildren = boundChildren,
                    selectedChildId = selectedChildId
                )
                HistoryFilters(
                    viewModel = viewModel,
                    historyYearMonth = historyYearMonth,
                    allSubjects = allSubjects,
                    historySubjectId = historySubjectId,
                    monthList = monthList
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (historySessions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无复习打卡历史记录", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            state = rememberLazyListState(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(historySessions) { session ->
                                TabletSessionCard(
                                    session = session,
                                    isSelected = session.id == selectedSessionId,
                                    onClick = { selectedSessionId = session.id }
                                )
                            }
                        }
                    }
                }
            }

            // Vertical Divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // Right Pane (60%): Detailed Question List
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(start = 16.dp)
            ) {
                if (selectedSessionId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👈 请在左侧选择一条复习记录", fontSize = 15.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("选择后即可在此处查阅该记录的题目列表", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        }
                    }
                } else if (loadingDetails) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF4F46E5))
                    }
                } else detailedSession?.let { session ->
                    SessionDetailPanel(
                        session = session,
                        onNavigateToNote = onNavigateToNote,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    } else {
        // --- Phone Vertical Stack Layout ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(16.dp)
        ) {
            Text("📋 复习记录列表", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(12.dp))
            ChildSelector(
                viewModel = viewModel,
                isParent = isParent,
                boundChildren = boundChildren,
                selectedChildId = selectedChildId
            )
            HistoryFilters(
                viewModel = viewModel,
                historyYearMonth = historyYearMonth,
                allSubjects = allSubjects,
                historySubjectId = historySubjectId,
                monthList = monthList
            )

            Box(modifier = Modifier.weight(1f)) {
                if (historySessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无复习打卡历史记录", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        state = rememberLazyListState(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(historySessions) { session ->
                            PhoneSessionCard(
                                session = session,
                                apiService = apiService,
                                onNavigateToNote = onNavigateToNote
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletSessionCard(
    session: ReviewSessionDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val formattedDate = remember(session.created_at) {
        if (!session.created_at.isNullOrBlank()) {
            session.created_at.replace("T", " ").substringBefore(".")
        } else {
            "未知时间"
        }
    }

    val statusColor = when (session.status) {
        "COMPLETED" -> Color(0xFF10B981)
        "IN_PROGRESS" -> Color(0xFF3B82F6)
        "ABANDONED" -> Color(0xFFEF4444)
        else -> Color(0xFF64748B)
    }

    val statusName = when (session.status) {
        "COMPLETED" -> "已完成"
        "IN_PROGRESS" -> "进行中"
        "ABANDONED" -> "已放弃"
        else -> session.status
    }

    val displayName = remember(session.semester_name, session.subject_name) {
        buildString {
            append(session.semester_name ?: "未知学期")
            if (!session.subject_name.isNullOrBlank()) {
                append(" · ")
                append(session.subject_name)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color(0xFF4F46E5) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEEF2FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Text(
                    text = "${session.completed_count} / ${session.target_count}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (session.status == "COMPLETED") Color(0xFF10B981) else Color(0xFF475569)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📅 $formattedDate",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun SessionDetailPanel(
    session: ReviewSessionDto,
    onNavigateToNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(session.created_at) {
        if (!session.created_at.isNullOrBlank()) {
            session.created_at.replace("T", " ").substringBefore(".")
        } else {
            "未知时间"
        }
    }

    val strategyName = when (session.strategy) {
        "RANDOM" -> "听天由命"
        "EBBINGHAUS" -> "艾宾浩斯"
        "CHALLENGE" -> "自我挑战"
        "PERSISTENT_FAILURE" -> "屡败屡战"
        else -> session.strategy
    }

    val statusColor = when (session.status) {
        "COMPLETED" -> Color(0xFF10B981)
        "IN_PROGRESS" -> Color(0xFF3B82F6)
        "ABANDONED" -> Color(0xFFEF4444)
        else -> Color(0xFF64748B)
    }

    val statusName = when (session.status) {
        "COMPLETED" -> "已完成"
        "IN_PROGRESS" -> "进行中"
        "ABANDONED" -> "已放弃"
        else -> session.status
    }

    val displayName = remember(session.semester_name, session.subject_name) {
        buildString {
            append(session.semester_name ?: "未知学期")
            if (!session.subject_name.isNullOrBlank()) {
                append(" · ")
                append(session.subject_name)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 $formattedDate  ·  策略: $strategyName",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Text(
                        text = "完成进度: ${session.completed_count} / ${session.target_count}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "题目详情列表:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val notesList = session.notes.orEmpty()
        if (notesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("无错题快照数据", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notesList.size) { idx ->
                    val note = notesList[idx]
                    QuestionListItem(note = note, index = idx + 1, onClick = { onNavigateToNote(note.id) })
                }
            }
        }
    }
}

@Composable
private fun QuestionListItem(
    note: NoteDto,
    index: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                modifier = Modifier.width(24.dp)
            )
            Text(
                text = "${note.subject_name ?: ""} · ${note.source_paper_name ?: note.question_source_name ?: "错题"}",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                modifier = Modifier.weight(1f)
            )
            val masteryText = when (note.mastery_level) {
                "CLUELESS" -> "完全不会"
                "PARTIAL" -> "一知半解"
                else -> "熟练掌握"
            }
            val masteryColor = when (note.mastery_level) {
                "CLUELESS" -> Color(0xFFEF4444)
                "PARTIAL" -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }
            Text(
                text = masteryText,
                fontSize = 12.sp,
                color = masteryColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..5).forEach { star ->
                    Text(
                        text = if (star <= note.difficulty_stars) "★" else "☆",
                        fontSize = 14.sp,
                        color = if (star <= note.difficulty_stars) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "预览 ▶",
                fontSize = 12.sp,
                color = Color(0xFF4F46E5),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PhoneSessionCard(
    session: ReviewSessionDto,
    apiService: com.sproutmemo.android.data.remote.ApiService,
    onNavigateToNote: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var detailedSession by remember { mutableStateOf<ReviewSessionDto?>(null) }
    var loadingDetails by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded && detailedSession == null) {
            loadingDetails = true
            try {
                val res = apiService.getReviewSessionDetails(session.id)
                if (res.isSuccessful && res.body()?.code == 200) {
                    detailedSession = res.body()?.data?.copy(
                        semester_name = session.semester_name,
                        subject_name = session.subject_name
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadingDetails = false
            }
        }
    }

    val formattedDate = remember(session.created_at) {
        if (!session.created_at.isNullOrBlank()) {
            session.created_at.replace("T", " ").substringBefore(".")
        } else {
            "未知时间"
        }
    }

    val strategyName = when (session.strategy) {
        "RANDOM" -> "听天由命"
        "EBBINGHAUS" -> "艾宾浩斯"
        "CHALLENGE" -> "自我挑战"
        "PERSISTENT_FAILURE" -> "屡败屡战"
        else -> session.strategy
    }

    val statusColor = when (session.status) {
        "COMPLETED" -> Color(0xFF10B981)
        "IN_PROGRESS" -> Color(0xFF3B82F6)
        "ABANDONED" -> Color(0xFFEF4444)
        else -> Color(0xFF64748B)
    }

    val statusName = when (session.status) {
        "COMPLETED" -> "已完成"
        "IN_PROGRESS" -> "进行中"
        "ABANDONED" -> "已放弃"
        else -> session.status
    }

    val displayName = remember(session.semester_name, session.subject_name) {
        buildString {
            append(session.semester_name ?: "未知学期")
            if (!session.subject_name.isNullOrBlank()) {
                append(" · ")
                append(session.subject_name)
            }
            append(" (共 ${session.target_count} 题)")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📅 $formattedDate · 策略: $strategyName",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text = "${session.completed_count} / ${session.target_count}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (session.status == "COMPLETED") Color(0xFF10B981) else Color(0xFF475569)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "错题详情列表:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (loadingDetails) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
                        }
                    } else {
                        val notes = detailedSession?.notes.orEmpty()
                        if (notes.isEmpty()) {
                            Text(
                                text = "无错题快照数据",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            notes.forEachIndexed { idx, note ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToNote(note.id) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Text(
                                        text = "${note.subject_name ?: ""} · ${note.source_paper_name ?: note.question_source_name ?: "错题"}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        (1..5).forEach { star ->
                                            Text(
                                                text = if (star <= note.difficulty_stars) "★" else "☆",
                                                fontSize = 13.sp,
                                                color = if (star <= note.difficulty_stars) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    val masteryText = when (note.mastery_level) {
                                        "CLUELESS" -> "完全不会"
                                        "PARTIAL" -> "一知半解"
                                        else -> "熟练掌握"
                                    }
                                    val masteryColor = when (note.mastery_level) {
                                        "CLUELESS" -> Color(0xFFEF4444)
                                        "PARTIAL" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF10B981)
                                    }
                                    Text(
                                        text = masteryText,
                                        fontSize = 11.sp,
                                        color = masteryColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "▶",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
