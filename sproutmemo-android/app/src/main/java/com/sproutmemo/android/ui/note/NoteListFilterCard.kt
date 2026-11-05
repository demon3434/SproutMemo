package com.sproutmemo.android.ui.note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun NoteListFilterCard(
    viewModel: NoteListViewModel,
    userRole: String,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val boundChildren by viewModel.boundChildren.collectAsState()
    val studentIdFilter by viewModel.studentId.collectAsState()

    val selectedStageId by viewModel.selectedStageId.collectAsState()
    val selectedSemesterId by viewModel.selectedSemesterId.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val selectedMastery by viewModel.selectedMastery.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val selectedUnitCode by viewModel.selectedUnitCode.collectAsState()
    val sourceType by viewModel.sourceType.collectAsState()

    val stages by viewModel.stages.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val units by viewModel.systemUnits.collectAsState()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (userRole == "PARENT") {
                NoteListFilterDropdown(
                    label = "选择孩子",
                    selectedValue = studentIdFilter ?: "",
                    options = listOf(Pair("", "全部")) + boundChildren.map { Pair(it.id, it.nickname ?: it.username) },
                    onSelected = { childId ->
                        val targetId = childId.ifBlank { null }
                        viewModel.setStudentIdFilter(targetId)
                        if (targetId != null) {
                            viewModel.initializeDefaultFilters(userRole, targetId)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val filteredSemesters = remember(semesters, selectedStageId) {
                if (selectedStageId.isBlank()) semesters else semesters.filter { it.stage_id == selectedStageId }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    NoteListFilterDropdown(
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
                    NoteListFilterDropdown(
                        label = "学期",
                        selectedValue = selectedSemesterId,
                        options = listOf(Pair("", "全部")) + filteredSemesters.map { Pair(it.id, it.name) },
                        onSelected = { viewModel.onSemesterSelected(it) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    NoteListFilterDropdown(
                        label = "学科",
                        selectedValue = selectedSubjectId,
                        options = listOf(Pair("", "全部")) + subjects.map { Pair(it.subject_id, it.subject?.subject_name ?: "") },
                        onSelected = { viewModel.selectedSubjectId.value = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    NoteListFilterDropdown(
                        label = "章节/单元",
                        selectedValue = selectedUnitCode,
                        options = listOf(Pair("", "全部")) + units.map { Pair(it.id.toString(), it.name) },
                        onSelected = { viewModel.selectedUnitCode.value = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    NoteListFilterDropdown(
                        label = "难度",
                        selectedValue = selectedDifficulty,
                        options = listOf(
                            Pair("", "全部"),
                            Pair("1", "★☆☆☆☆ (1星)"),
                            Pair("2", "★★☆☆☆ (2星)"),
                            Pair("3", "★★★☆☆ (3星)"),
                            Pair("4", "★★★★☆ (4星)"),
                            Pair("5", "★★★★★ (5星)")
                        ),
                        onSelected = { viewModel.selectedDifficulty.value = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    NoteListFilterDropdown(
                        label = "掌握程度",
                        selectedValue = selectedMastery,
                        options = listOf(
                            Pair("", "全部"),
                            Pair("CLUELESS", "🔴 完全不会"),
                            Pair("PARTIAL", "🟡 一知半解"),
                            Pair("MASTERED", "🟢 熟练掌握")
                        ),
                        onSelected = { viewModel.selectedMastery.value = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            NoteListFilterDropdown(
                label = "来源类型",
                selectedValue = sourceType,
                options = listOf(
                    Pair("", "全部"),
                    Pair("BOOK", "📖 习题册"),
                    Pair("PAPER", "📄 试卷/其它")
                ),
                onSelected = { viewModel.sourceType.value = it }
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.fetchNotes(resetPage = true)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                ) {
                    Text("应用筛选", fontSize = 12.sp, color = Color.White)
                }
                OutlinedButton(
                    onClick = {
                        viewModel.selectedStageId.value = ""
                        viewModel.selectedSemesterId.value = ""
                        viewModel.selectedSubjectId.value = ""
                        viewModel.selectedUnitCode.value = ""
                        viewModel.selectedDifficulty.value = ""
                        viewModel.selectedMastery.value = ""
                        viewModel.sourceType.value = ""
                        viewModel.subjects.value = emptyList()
                        if (userRole == "PARENT") {
                            viewModel.initializeDefaultFilters(userRole, null)
                        } else {
                            viewModel.fetchNotes(resetPage = true)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                ) {
                    Text("重置", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListFilterDropdown(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.find { it.first == selectedValue }?.second ?: "全部"
    val colors = LocalAppColors.current

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
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.neonBlue,
                unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.5f)
            ),
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

