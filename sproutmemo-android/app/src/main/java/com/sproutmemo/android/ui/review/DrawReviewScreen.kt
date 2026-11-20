package com.sproutmemo.android.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.data.remote.model.ReviewSessionDto
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.StationeryPrimaryButton
import com.sproutmemo.android.ui.components.gridPaper
import com.sproutmemo.android.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DrawReviewScreen(
    viewModel: ReviewViewModel,
    onStartPractice: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val reviewState by viewModel.reviewState.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val selectedStageId by viewModel.selectedStageId.collectAsState()
    val selectedSemesterId by viewModel.selectedSemesterId.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val selectedUnitCode by viewModel.selectedUnitCode.collectAsState()
    val selectedCountPresetCode by viewModel.selectedCountPresetCode.collectAsState()
    val selectedStrategy by viewModel.selectedStrategy.collectAsState()

    val stages by viewModel.stages.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val units by viewModel.units.collectAsState()
    val presetsData by viewModel.presetsData.collectAsState()

    // Filter semesters based on stage
    val filteredSemesters = remember(semesters, selectedStageId) {
        if (selectedStageId.isBlank()) semesters else semesters.filter { it.stage_id == selectedStageId }
    }

    val activeSession by viewModel.activeSessionDetected.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkActiveSession()
    }

    // Auto navigate when session active
    LaunchedEffect(reviewState) {
        val state = reviewState
        if (state is ReviewState.Practicing) {
            onStartPractice(state.session.id)
        }
    }

    if (activeSession != null) {
        AlertDialog(
            onDismissRequest = { /* Force user action, no outside click dismiss */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StationeryIcon(name = "list", size = 18.dp, tint = colors.neonBlue)
                    Text("发现未完成的复习", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
            },
            text = { Text("您上次有未完成的复习任务（共 ${activeSession?.target_count} 题，已完成 ${activeSession?.completed_count} 题），是否继续答题？", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        activeSession?.let {
                            viewModel.resumeSession(it)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                ) {
                    Text("继续答题", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        activeSession?.let {
                            viewModel.abandonSession(it.id)
                        }
                    }
                ) {
                    Text("放弃并新建", color = colors.neonRed)
                }
            },
            containerColor = colors.cardBg
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .gridPaper(colors.darkBg, colors.rule)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StationeryIcon(name = "target", size = 22.dp, tint = colors.neonBlue)
                Text("抽题复习打卡", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
            Spacer(modifier = Modifier.height(14.dp))

            when (val state = reviewState) {
                is ReviewState.Initial -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        StationeryCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("抽题过滤条件", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.neonBlue)
                                Spacer(modifier = Modifier.height(14.dp))

                                // 第一排：学习阶段 & 学期 (横向并列)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ReviewFilterDropdown(
                                            label = "学习阶段",
                                            selectedValue = selectedStageId,
                                            options = listOf(Pair("", "全部")) + stages.map { Pair(it.id, it.name) },
                                            onSelected = {
                                                viewModel.selectedStageId.value = it
                                                viewModel.onSemesterSelected("")
                                            }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        ReviewFilterDropdown(
                                            label = "学期",
                                            selectedValue = selectedSemesterId,
                                            options = listOf(Pair("", "全部")) + filteredSemesters.map { Pair(it.id, it.name) },
                                            onSelected = { viewModel.onSemesterSelected(it) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 第二排：学科 & 单元/阶段 (横向并列)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ReviewFilterDropdown(
                                            label = "学科",
                                            selectedValue = selectedSubjectId,
                                            options = listOf(Pair("", "全部")) + subjects.map { Pair(it.subject_id, it.subject?.subject_name ?: "") },
                                            onSelected = { viewModel.selectedSubjectId.value = it }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        val unitOptions = listOf(Pair("", "全部单元")) + units.map { Pair(it.id, it.name) }
                                        val selectedUnitDisplayId = selectedUnitCode?.toString() ?: ""
                                        ReviewFilterDropdown(
                                            label = "单元/阶段",
                                            selectedValue = selectedUnitDisplayId,
                                            options = unitOptions,
                                            onSelected = {
                                                viewModel.selectedUnitCode.value = it.toIntOrNull()
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 第三排：筛题策略 (并列放问号按钮)
                                var showStrategyHelp by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(0.85f)) {
                                        val strategyOptions = presetsData?.strategies?.map {
                                            Pair(it["code"] ?: "", it["name"] ?: "")
                                        } ?: listOf(
                                            Pair("RANDOM", "听天由命"),
                                            Pair("EBBINGHAUS", "艾宾浩斯"),
                                            Pair("CHALLENGE", "自我挑战"),
                                            Pair("PERSISTENT_FAILURE", "屡败屡战")
                                        )
                                        ReviewFilterDropdown(
                                            label = "筛题策略",
                                            selectedValue = selectedStrategy,
                                            options = strategyOptions,
                                            onSelected = { viewModel.selectedStrategy.value = it }
                                        )
                                    }
                                    IconButton(
                                        onClick = { showStrategyHelp = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                            contentDescription = "策略说明",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                if (showStrategyHelp) {
                                    AlertDialog(
                                        onDismissRequest = { showStrategyHelp = false },
                                        title = {
                                            Text(
                                                text = "筛题策略含义说明",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = colors.textPrimary
                                            )
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text("• 听天由命: 纯随机从题库中抽取错题。", fontSize = 13.sp, color = colors.textSecondary)
                                                Text("• 艾宾浩斯: 基于艾宾浩斯遗忘曲线智能编排，优先抽取最需复习的错题。", fontSize = 13.sp, color = colors.textSecondary)
                                                Text("• 自我挑战: 挑选近期复习记录中错误率最高、难度最大的高阻错题进行突破训练。", fontSize = 13.sp, color = colors.textSecondary)
                                                Text("• 屡败屡战: 优先调度近期复习记录中连续出错且最难突破的顽固错题。", fontSize = 13.sp, color = colors.textSecondary)
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showStrategyHelp = false }) {
                                                Text("确 定", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = colors.cardBg
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = colors.rule)
                                Spacer(modifier = Modifier.height(16.dp))

                                // 题量选择：换成 Choice Chips 胶囊选择器
                                Text("选择复习题量", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val countPresetOptions = presetsData?.count_presets?.map { preset ->
                                    Pair(preset.code, preset.name)
                                } ?: listOf(
                                    Pair("PRESET_3", "蜻蜓点水 (3题)"),
                                    Pair("PRESET_5", "稳扎稳打 (5题)"),
                                    Pair("PRESET_8", "精益求精 (8题)"),
                                    Pair("PRESET_10", "扶摇直上 (10题)")
                                )
                                
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    countPresetOptions.forEach { (code, rawName) ->
                                        val displayName = rawName.split(" ")[0]
                                        val countVal = when (code) {
                                            "PRESET_3" -> "3"
                                            "PRESET_5" -> "5"
                                            "PRESET_8" -> "8"
                                            "PRESET_10" -> "10"
                                            else -> "5"
                                        }
                                        val isSelected = selectedCountPresetCode == code
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.selectedCountPresetCode.value = code },
                                            label = {
                                                Text("$displayName (${countVal}题)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colors.neonBlue.copy(alpha = 0.15f),
                                                selectedLabelColor = colors.neonBlue,
                                                containerColor = colors.paper2,
                                                labelColor = colors.textSecondary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                val isSubmitEnabled = selectedSemesterId.isNotBlank()
                                if (loading) {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                                    ) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    StationeryPrimaryButton(
                                        text = if (isSubmitEnabled) "确认抽取并生成大纲" else "请先选择学期",
                                        iconName = if (isSubmitEnabled) "target" else null,
                                        enabled = isSubmitEnabled,
                                        onClick = { viewModel.generatePreview() },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
                is ReviewState.Previewing -> {
                    StationeryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StationeryIcon(name = "list", size = 18.dp, tint = colors.neonBlue)
                                Text("本次复习题目大纲 (共 ${state.notes.size} 题)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.notes.forEachIndexed { idx, note ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.paper2, shape = RoundedCornerShape(10.dp))
                                            .border(1.dp, colors.rule, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${idx + 1}.", fontWeight = FontWeight.Bold, color = colors.neonBlue, modifier = Modifier.width(28.dp), fontSize = 14.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = note.subject_name ?: "",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textSecondary
                                            )
                                            Text(
                                                text = note.source_paper_name ?: note.question_source_name ?: "未命名错题",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            StationeryIcon(name = "star-filled", size = 14.dp, tint = colors.neonAmber)
                                            Text("${note.difficulty_stars.toInt()}", fontSize = 13.sp, color = colors.neonAmber, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.startFormalSession(state.notes) },
                                modifier = Modifier.weight(1.5f).height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.neonGreen)
                            ) {
                                StationeryIcon(name = "pen", size = 16.dp, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("开始答题", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetReview() },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("重新选择", color = colors.textPrimary)
                            }
                        }
                    }
                }
            }
            is ReviewState.Practicing -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.neonBlue)
                }
            }
            is ReviewState.Completed -> {
                StationeryCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StationeryIcon(name = "mastery-mastered", size = 40.dp, tint = colors.neonGreen)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("恭喜！完成本次复习打卡！", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.neonGreen)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "本次共完成 ${state.session.target_count} 道题目复习打卡，复习轨迹已存入数据库。",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        StationeryPrimaryButton(
                            text = "再次抽题复习",
                            iconName = "target",
                            onClick = { viewModel.resetReview() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
}

