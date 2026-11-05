package com.sproutmemo.android.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.model.QuestionBookDto
import com.sproutmemo.android.ui.theme.LocalAppColors
import com.sproutmemo.android.ui.components.StyledDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookManageScreen(viewModel: BookManageViewModel) {
    val colors = LocalAppColors.current

    val userRole by viewModel.userRole.collectAsState()
    val boundChildren by viewModel.boundChildren.collectAsState()
    val selectedChildId by viewModel.selectedChildId.collectAsState()

    val stages by viewModel.stages.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val filteredSemesters by viewModel.filteredSemesters.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val books by viewModel.books.collectAsState()

    val selectedStageId by viewModel.selectedStageId.collectAsState()
    val selectedSemesterId by viewModel.selectedSemesterId.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val newBookName by viewModel.newBookName.collectAsState()
    val addError by viewModel.addError.collectAsState()

    var bookToDelete by remember { mutableStateOf<QuestionBookDto?>(null) }
    var editingBook by remember { mutableStateOf<QuestionBookDto?>(null) }
    var editingName by remember { mutableStateOf("") }

    // Double Alert Dialogs
    bookToDelete?.let { book ->
        BookDeleteDialog(
            book = book,
            onConfirm = {
                viewModel.deleteBook(book.id)
                bookToDelete = null
            },
            onDismiss = { bookToDelete = null }
        )
    }

    editingBook?.let { book ->
        BookRenameDialog(
            name = editingName,
            onNameChange = { editingName = it },
            onConfirm = {
                viewModel.renameBook(book.id, editingName)
                editingBook = null
            },
            onDismiss = { editingBook = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.darkBg)
            .padding(16.dp)
    ) {
        Text(
            text = if (userRole == "PARENT") "📖 孩子的习题册" else "📖 我的习题册",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Parent Child Selector Card (Only shown for PARENT)
        if (userRole == "PARENT" && boundChildren.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("选择学生账号:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    var childExpanded by remember { mutableStateOf(false) }
                    val activeChild = boundChildren.find { it.id == selectedChildId }
                    val childName = activeChild?.nickname ?: activeChild?.username ?: "选择孩子"
                    Box {
                        OutlinedButton(
                            onClick = { childExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.neonBlue)
                        ) {
                            Text(childName, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = childExpanded,
                            onDismissRequest = { childExpanded = false }
                        ) {
                            boundChildren.forEach { child ->
                                DropdownMenuItem(
                                    text = { Text(child.nickname ?: child.username) },
                                    onClick = {
                                        viewModel.onChildSelected(child.id)
                                        childExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Cascade Selectors (Stage and Semester)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val currentStage = stages.find { it.id == selectedStageId }
            StyledDropdown(
                label = "学习阶段",
                items = stages,
                selectedItem = currentStage,
                itemToString = { it.name },
                onItemSelected = { viewModel.onStageChanged(it.id) },
                modifier = Modifier.weight(1f)
            )

            val currentSem = semesters.find { it.id == selectedSemesterId }
            StyledDropdown(
                label = "学期",
                items = filteredSemesters,
                selectedItem = currentSem,
                itemToString = { it.name },
                onItemSelected = { viewModel.onSemesterChanged(it.id) },
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Subject Options Tabs Group
        if (selectedSemesterId.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            if (subjects.isEmpty()) {
                Text(
                    text = "⚠️ 该学期暂未配置任何关联学科，请切换学期或联系管理员。",
                    color = colors.neonAmber,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { subMapping ->
                        val isSelected = selectedSubjectId == subMapping.subject_id
                        val subjectName = subMapping.subject?.subject_name ?: "未知"
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onSubjectSelected(subMapping.subject_id) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(subjectName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (isSelected && books.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .background(colors.neonBlue, shape = RoundedCornerShape(10.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = books.size.toString(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.neonBlue.copy(alpha = 0.15f),
                                selectedLabelColor = colors.neonBlue
                            )
                        )
                    }
                }
            }
        }

        // 4. Quick Add Form Card
        if (selectedSubjectId.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            BookQuickAddCard(
                bookName = newBookName,
                onBookNameChange = { viewModel.newBookName.value = it },
                onAdd = { viewModel.addBook() },
                addError = addError
            )
        }

        // 5. Books List Section
        Spacer(modifier = Modifier.height(12.dp))
        if (selectedSubjectId.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (userRole == "PARENT") "💡 请选择学生、学期及学科以加载已存书柜" else "💡 请选择学期以及学科以加载我的书柜",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            }
        } else if (books.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("📖 当前学科下暂无习题册，请在上方快速添加。", color = colors.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(books) { index, book ->
                    BookItemCard(
                        book = book,
                        index = index,
                        totalCount = books.size,
                        onMove = { direction -> viewModel.moveBook(book, direction) },
                        onEdit = {
                            editingBook = book
                            editingName = book.name
                        },
                        onToggle = { viewModel.toggleBookActive(book) },
                        onDelete = { bookToDelete = book }
                    )
                }
            }
        }
    }
}
