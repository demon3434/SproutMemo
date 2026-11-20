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

    val colors = LocalAppColors.current
    if (isTabletLandscape) {
        // --- Tablet Dual Column Layout ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .gridPaper(colors.darkBg, colors.rule)
                .padding(16.dp)
        ) {
            // Left Pane (40%): Filters and Sessions List
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StationeryIcon(name = "list", size = 20.dp, tint = colors.neonBlue)
                    Text("往期复习任务", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StationeryIcon(name = "chevron-left", size = 16.dp, tint = colors.textSecondary)
                                Text("请在左侧选择一条复习记录", fontSize = 15.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("选择后即可在此处查阅该记录的题目列表", fontSize = 13.sp, color = colors.textSecondary.copy(alpha = 0.7f))
                        }
                    }
                } else if (loadingDetails) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.neonBlue)
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
                .gridPaper(colors.darkBg, colors.rule)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StationeryIcon(name = "list", size = 20.dp, tint = colors.neonBlue)
                Text("复习记录列表", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
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

