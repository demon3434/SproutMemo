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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DrawReviewScreen(
    viewModel: ReviewViewModel,
    onStartPractice: (String) -> Unit
) {
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
            title = { Text("📋 发现未完成的复习", fontWeight = FontWeight.Bold) },
            text = { Text("您上次有未完成的复习任务（共 ${activeSession?.target_count} 题，已完成 ${activeSession?.completed_count} 题），是否继续答题？") },
            confirmButton = {
                Button(
                    onClick = {
                        activeSession?.let {
                            viewModel.resumeSession(it)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
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
                    Text("放弃并新建", color = Color(0xFFEF4444))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
        ) {
            Text("🎯 抽题复习打卡", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(14.dp))

            when (val state = reviewState) {
                is ReviewState.Initial -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("抽题过滤条件", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
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
                                                text = "💡 筛题策略含义说明",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            )
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text("• 听天由命: 纯随机从题库中抽取错题。", fontSize = 13.sp)
                                                Text("• 艾宾浩斯: 基于艾宾浩斯遗忘曲线智能编排，优先抽取最需复习的错题。", fontSize = 13.sp)
                                                Text("• 自我挑战: 挑选近期复习记录中错误率最高、难度最大的高阻错题进行突破训练。", fontSize = 13.sp)
                                                Text("• 屡败屡战: 优先调度近期复习记录中连续出错且最难突破的顽固错题。", fontSize = 13.sp)
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showStrategyHelp = false }) {
                                                Text("确 定", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(16.dp))

                                // 题量选择：换成 Choice Chips 胶囊选择器
                                Text("选择复习题量", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
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
                                                Text("$displayName (${countVal}题)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF4F46E5).copy(alpha = 0.15f),
                                                selectedLabelColor = Color(0xFF4F46E5),
                                                containerColor = Color(0xFFF1F5F9),
                                                labelColor = Color(0xFF475569)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                val isSubmitEnabled = selectedSemesterId.isNotBlank()
                                Button(
                                    onClick = { viewModel.generatePreview() },
                                    enabled = !loading && isSubmitEnabled,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                ) {
                                    if (loading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text(
                                            if (isSubmitEnabled) "🚀 确认抽取并生成大纲" else "⚠️ 请先选择学期",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is ReviewState.Previewing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📋 本次复习题目大纲 (共 ${state.notes.size} 题)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(14.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.notes.forEachIndexed { idx, note ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${idx + 1}.", fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5), modifier = Modifier.width(28.dp), fontSize = 14.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = note.subject_name ?: "",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                            Text(
                                                text = note.source_paper_name ?: note.question_source_name ?: "未命名错题",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                        Text("★ ${note.difficulty_stars.toInt()}", fontSize = 13.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.startFormalSession(state.notes) },
                                modifier = Modifier.weight(1.5f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("✍️ 开始答题", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetReview() },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("重新选择")
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
                    CircularProgressIndicator(color = Color(0xFF4F46E5))
                }
            }
            is ReviewState.Completed -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆 恭喜！完成本次复习打卡！", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "本次共完成 ${state.session.target_count} 道题目复习打卡，复习轨迹已存入数据库。",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.resetReview() },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Text("🔄 再次抽题复习", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewFilterDropdown(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.find { it.first == selectedValue }?.second ?: "全部"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.second) },
                    onClick = {
                        onSelected(opt.first)
                        expanded = false
                    }
                )
            }
        }
    }
}
