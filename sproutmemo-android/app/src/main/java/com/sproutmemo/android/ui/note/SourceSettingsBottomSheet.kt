package com.sproutmemo.android.ui.note

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.LocalAppColors
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import com.sproutmemo.android.ui.components.StyledDropdown
import com.sproutmemo.android.ui.components.RequiredLabel
import com.sproutmemo.android.ui.components.SelectableOptionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSettingsBottomSheet(
    viewModel: NoteFormViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    val stages by viewModel.stages.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val books by viewModel.books.collectAsState()
    val units by viewModel.filteredUnits.collectAsState()
    val paperCategories by viewModel.paperCategories.collectAsState()
    val paperSuggestions by viewModel.paperSuggestions.collectAsState()
    val showSuggestions by viewModel.showSuggestions.collectAsState()

    val selectedStageId by viewModel.selectedStageId.collectAsState()
    val selectedSemesterId by viewModel.selectedSemesterId.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val sourceType by viewModel.sourceType.collectAsState()
    val selectedQuestionSourceId by viewModel.selectedQuestionSourceId.collectAsState()
    val paperCategory by viewModel.paperCategory.collectAsState()
    val sourcePaperName by viewModel.sourcePaperName.collectAsState()
    val selectedUnitCode by viewModel.selectedUnitCode.collectAsState()
    val pageNumber by viewModel.pageNumber.collectAsState()

    // Sliding visibility state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val coroutineScope = rememberCoroutineScope()
    val handleDismiss = {
        isVisible = false
        coroutineScope.launch {
            kotlinx.coroutines.delay(250)
            onDismiss()
        }
    }

    val handleConfirm = {
        isVisible = false
        coroutineScope.launch {
            kotlinx.coroutines.delay(250)
            onConfirm()
        }
    }

    // Intercept hardware and gesture back (same behavior as back button)
    BackHandler {
        handleDismiss()
    }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 250)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 250)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.darkBg
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Bar with Back Button and Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .background(colors.cardBg)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { handleDismiss() }) {
                            Text("←", fontSize = 24.sp, color = colors.textPrimary)
                        }
                        Text(
                            text = "关联习题来源",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Content List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // 1. Cascading Selectors (Stage -> Semester -> Subject)
                        Text("学习阶段 / 学期 / 学科", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Stage Dropdown
                            val currentStage = stages.find { it.id == selectedStageId }
                            StyledDropdown(
                                label = "学习阶段",
                                items = stages,
                                selectedItem = currentStage,
                                itemToString = { it.name },
                                onItemSelected = { viewModel.onStageChanged(it.id) },
                                modifier = Modifier.weight(1f),
                                isRequired = true
                            )

                            // Semester Dropdown
                            val filteredSems = remember(semesters, selectedStageId) {
                                semesters.filter { it.stage_id == selectedStageId }
                            }
                            val currentSem = filteredSems.find { it.id == selectedSemesterId }
                            StyledDropdown(
                                label = "学期",
                                items = filteredSems,
                                selectedItem = currentSem,
                                itemToString = { it.name },
                                onItemSelected = { viewModel.onSemesterChanged(it.id) },
                                modifier = Modifier.weight(1f),
                                isRequired = true
                            )

                            // Subject Dropdown
                            val currentSub = subjects.find { it.subject_id == selectedSubjectId }
                            StyledDropdown(
                                label = "学科",
                                items = subjects,
                                selectedItem = currentSub,
                                itemToString = { it.subject?.subject_name ?: "未知" },
                                onItemSelected = { viewModel.onSubjectChanged(it.subject_id) },
                                modifier = Modifier.weight(1f),
                                isRequired = true
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Source Type Selector (BOOK vs PAPER)
                        Text("来源类型", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SelectableOptionCard(
                                label = "习题册",
                                iconText = "📖",
                                isSelected = sourceType == "BOOK",
                                onClick = { viewModel.sourceType.value = "BOOK" },
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                            SelectableOptionCard(
                                label = "试卷/其它",
                                iconText = "📄",
                                isSelected = sourceType == "PAPER",
                                onClick = { viewModel.sourceType.value = "PAPER" },
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Conditional Layout based on Source Type
                        if (sourceType == "BOOK") {
                            // Book selector
                            val currentBook = books.find { it.id == selectedQuestionSourceId }
                            StyledDropdown(
                                label = "关联已存习题册",
                                items = books,
                                selectedItem = currentBook,
                                itemToString = { it.name },
                                onItemSelected = { viewModel.selectedQuestionSourceId.value = it.id },
                                isRequired = true
                            )
                            if (books.isEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ 当前学科下暂无已创建的习题册。录题只能选择已有习题册，请先前往「我的习题册」录入书架。",
                                    color = colors.neonAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Chapter/Unit selector
                                val currentUnit = units.find { it.id == selectedUnitCode }
                                StyledDropdown(
                                    label = "章节单元",
                                    items = units,
                                    selectedItem = currentUnit,
                                    itemToString = { it.name },
                                    onItemSelected = { viewModel.selectedUnitCode.value = it.id },
                                    modifier = Modifier.weight(1f),
                                    isRequired = true
                                )

                                // Page number
                                OutlinedTextField(
                                    value = pageNumber,
                                    onValueChange = { viewModel.pageNumber.value = it },
                                    label = { RequiredLabel("页码 (如 P.45)", isRequired = true) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary,
                                        focusedBorderColor = colors.neonBlue,
                                        unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.3f),
                                        focusedLabelColor = colors.neonBlue,
                                        unfocusedLabelColor = colors.textSecondary
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        } else {
                            // Paper Category selector
                            val currentCat = paperCategories.find { it.code == paperCategory }
                            StyledDropdown(
                                label = "试卷分类",
                                items = paperCategories,
                                selectedItem = currentCat,
                                itemToString = { it.name },
                                onItemSelected = {
                                    viewModel.paperCategory.value = it.code
                                    viewModel.fetchPaperSuggestions(sourcePaperName)
                                },
                                isRequired = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Chapter/Unit selector
                            val currentUnit = units.find { it.id == selectedUnitCode }
                            StyledDropdown(
                                label = "章节单元",
                                items = units,
                                selectedItem = currentUnit,
                                itemToString = { it.name },
                                onItemSelected = {
                                    viewModel.selectedUnitCode.value = it.id
                                    viewModel.fetchPaperSuggestions(sourcePaperName)
                                },
                                isRequired = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Paper Name with Autocomplete
                            var isFocused by remember { mutableStateOf(false) }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = sourcePaperName,
                                    onValueChange = {
                                        viewModel.sourcePaperName.value = it
                                        viewModel.fetchPaperSuggestions(it)
                                        viewModel.showSuggestions.value = true
                                    },
                                    label = { RequiredLabel("自定义来源/试卷名称", isRequired = true) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary,
                                        focusedBorderColor = colors.neonBlue,
                                        unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.3f),
                                        focusedLabelColor = colors.neonBlue,
                                        unfocusedLabelColor = colors.textSecondary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isFocused = it.isFocused },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                if (isFocused && showSuggestions && paperSuggestions.isNotEmpty()) {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                                        border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .heightIn(max = 160.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .verticalScroll(rememberScrollState())
                                                .padding(4.dp)
                                        ) {
                                            paperSuggestions.forEach { item ->
                                                Text(
                                                    text = item.name,
                                                    color = colors.textPrimary,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.sourcePaperName.value = item.name
                                                            viewModel.showSuggestions.value = false
                                                        }
                                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (selectedStageId.isBlank()) {
                                    Toast.makeText(context, "请选择学习阶段", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedSemesterId.isBlank()) {
                                    Toast.makeText(context, "请选择学期", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedSubjectId.isBlank()) {
                                    Toast.makeText(context, "请选择学科", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (sourceType == "BOOK") {
                                    if (selectedQuestionSourceId.isBlank()) {
                                        Toast.makeText(context, "请选择关联的已存习题册", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (selectedUnitCode.isBlank()) {
                                        Toast.makeText(context, "请选择章节单元", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (pageNumber.isBlank()) {
                                        Toast.makeText(context, "请输入页码", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!pageNumber.any { it.isDigit() }) {
                                        Toast.makeText(context, "页码格式不正确，必须包含数字数值", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                } else {
                                    if (paperCategory.isBlank()) {
                                        Toast.makeText(context, "请选择试卷分类", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (selectedUnitCode.isBlank()) {
                                        Toast.makeText(context, "请选择章节单元", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (sourcePaperName.isBlank()) {
                                        Toast.makeText(context, "请输入自定义来源/试卷名称", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }

                                handleConfirm()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                        ) {
                            Text("确定", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

