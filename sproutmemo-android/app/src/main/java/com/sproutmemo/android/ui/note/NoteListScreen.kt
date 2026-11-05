package com.sproutmemo.android.ui.note

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.ui.components.NoteImageLightboxDialog
import com.sproutmemo.android.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    userRole: String,
    isTabletLandscape: Boolean,
    onEditNote: (String) -> Unit
) {
    val selectedNoteDetail by viewModel.selectedNoteDetail.collectAsState()
    val colors = LocalAppColors.current

    var showLightbox by remember { mutableStateOf(false) }
    var lightboxUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var lightboxSelectedIndex by remember { mutableStateOf(0) }

    val semesters by viewModel.semesters.collectAsState()
    LaunchedEffect(semesters.size) {
        if (semesters.isNotEmpty()) {
            viewModel.initializeDefaultFilters(userRole, viewModel.studentId.value)
        }
    }

    if (isTabletLandscape) {
        Row(modifier = Modifier.fillMaxSize().background(colors.darkBg)) {
            Box(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(8.dp)) {
                NoteListColumn(
                    viewModel = viewModel,
                    userRole = userRole,
                    selectedNoteId = selectedNoteDetail?.id,
                    onSelectNote = { viewModel.selectedNoteDetail.value = it }
                )
            }

            Box(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(8.dp)) {
                NoteDetailPane(
                    note = selectedNoteDetail,
                    userRole = userRole,
                    viewModel = viewModel,
                    onDelete = {
                        viewModel.deleteNote(it)
                        viewModel.selectedNoteDetail.value = null
                    },
                    onEdit = onEditNote,
                    onOpenLightbox = { urls, index ->
                        lightboxUrls = urls
                        lightboxSelectedIndex = index
                        showLightbox = true
                    }
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(colors.darkBg).padding(12.dp)) {
            NoteListColumn(
                viewModel = viewModel,
                userRole = userRole,
                selectedNoteId = selectedNoteDetail?.id,
                onSelectNote = { viewModel.selectedNoteDetail.value = it }
            )
        }

        val noteToDetail = selectedNoteDetail
        if (noteToDetail != null) {
            Dialog(
                onDismissRequest = { viewModel.selectedNoteDetail.value = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                var animatedVisible by remember { mutableStateOf(false) }
                LaunchedEffect(noteToDetail) {
                    animatedVisible = true
                }

                val coroutineScope = rememberCoroutineScope()
                val dismissWithAnimation = {
                    coroutineScope.launch {
                        animatedVisible = false
                        kotlinx.coroutines.delay(220)
                        viewModel.selectedNoteDetail.value = null
                    }
                }

                androidx.activity.compose.BackHandler(enabled = animatedVisible) {
                    dismissWithAnimation()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.darkBg)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = animatedVisible,
                        enter = androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(durationMillis = 220)
                        ),
                        exit = androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(durationMillis = 220)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.cardBg)
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { dismissWithAnimation() }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = colors.textPrimary
                                    )
                                }
                                Text(
                                    text = "查看习题",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                NoteDetailPane(
                                    note = noteToDetail,
                                    userRole = userRole,
                                    viewModel = viewModel,
                                    onDelete = {
                                        viewModel.deleteNote(it)
                                        dismissWithAnimation()
                                    },
                                    onEdit = {
                                        dismissWithAnimation()
                                        onEditNote(it)
                                    },
                                    onOpenLightbox = { urls, index ->
                                        lightboxUrls = urls
                                        lightboxSelectedIndex = index
                                        showLightbox = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Image Lightbox with Floating Left/Right Rotate & Persistent Saving
    if (showLightbox && lightboxUrls.isNotEmpty()) {
        val currentImageAttachments = remember(selectedNoteDetail?.id, selectedNoteDetail?.attachments) {
            selectedNoteDetail?.attachments?.filter {
                it.file_type?.equals("IMAGE", ignoreCase = true) == true
            } ?: emptyList()
        }
        NoteImageLightboxDialog(
            imageUrls = lightboxUrls,
            initialIndex = lightboxSelectedIndex,
            noteId = selectedNoteDetail?.id,
            attachmentIds = currentImageAttachments.map { it.id },
            onNoteUpdated = { updatedNote ->
                viewModel.applyUpdatedNote(updatedNote)
            },
            onDismiss = { showLightbox = false }
        )
    }
}

@Composable
fun NoteListColumn(
    viewModel: NoteListViewModel,
    userRole: String,
    selectedNoteId: String?,
    onSelectNote: (NoteDto) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val colors = LocalAppColors.current

    var showFilters by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("搜索...", color = colors.textSecondary) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.fetchNotes(resetPage = true) }) {
                        Text("🔍", color = colors.textPrimary)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.neonBlue,
                    unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedIconButton(
                onClick = { showFilters = !showFilters },
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = if (showFilters) colors.neonBlue.copy(alpha = 0.15f) else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Filter",
                    tint = colors.textPrimary
                )
            }
        }

        if (showFilters) {
            Spacer(modifier = Modifier.height(8.dp))
            NoteListFilterCard(
                viewModel = viewModel,
                userRole = userRole,
                onDismiss = { showFilters = false }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.neonBlue)
            }
        } else if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("📂 暂无符合条件的错题笔记", color = colors.textSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(notes) { note ->
                    val isSelected = note.id == selectedNoteId
                    val containerBg = if (isSelected) colors.neonBlue.copy(alpha = 0.15f) else colors.cardBg
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectNote(note) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = containerBg
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${note.subject_name ?: "学科"} · ${note.semester_name ?: ""}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.neonBlue
                                )
                                Text(
                                    text = when (note.mastery_level) {
                                        "CLUELESS" -> "🔴 完全不会"
                                        "PARTIAL" -> "🟡 一知半解"
                                        else -> "🟢 熟练掌握"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${if (note.source_type == "BOOK") "📖 " else "📄 "}${note.question_source_name ?: note.source_paper_name ?: "自定义来源"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )

                            Text(
                                text = note.auxiliary_text ?: "(暂无文字描述)",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.created_at?.substringBefore('T') ?: "",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary.copy(alpha = 0.7f)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val imagesCount = note.attachments.count { it.file_type?.equals("IMAGE", ignoreCase = true) == true }
                                    val audiosCount = note.attachments.count { it.file_type?.equals("AUDIO", ignoreCase = true) == true }
                                    val videosCount = note.attachments.count { it.file_type?.equals("VIDEO", ignoreCase = true) == true }

                                    if (imagesCount > 0) {
                                        Text("🖼️ $imagesCount", fontSize = 11.sp, color = colors.textPrimary)
                                    }
                                    if (audiosCount > 0) {
                                        Text("🎵 $audiosCount", fontSize = 11.sp, color = colors.textPrimary)
                                    }
                                    if (videosCount > 0) {
                                        Text("📹 $videosCount", fontSize = 11.sp, color = colors.textPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pagination Bar
            val currentPage by viewModel.page.collectAsState()
            val totalPages by viewModel.totalPages.collectAsState()
            val totalItems by viewModel.totalItems.collectAsState()

            if (totalPages > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentPage > 1) {
                                viewModel.page.value = currentPage - 1
                                viewModel.fetchNotes()
                            }
                        },
                        enabled = currentPage > 1,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("上一页", fontSize = 12.sp)
                    }

                    Text(
                        text = "第 $currentPage / $totalPages 页 (共 $totalItems 条)",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )

                    OutlinedButton(
                        onClick = {
                            if (currentPage < totalPages) {
                                viewModel.page.value = currentPage + 1
                                viewModel.fetchNotes()
                            }
                        },
                        enabled = currentPage < totalPages,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("下一页", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
