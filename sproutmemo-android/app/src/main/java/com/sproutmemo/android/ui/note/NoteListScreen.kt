package com.sproutmemo.android.ui.note

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.ui.components.NoteImageLightboxDialog
import com.sproutmemo.android.ui.components.StarRatingStickerBar
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.StationeryWashiTape
import com.sproutmemo.android.ui.components.gridPaper
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
        Row(modifier = Modifier.fillMaxSize().gridPaper(colors.darkBg, colors.rule)) {
            Box(modifier = Modifier.weight(0.34f).fillMaxHeight().padding(8.dp)) {
                NoteListColumn(
                    viewModel = viewModel,
                    userRole = userRole,
                    selectedNoteId = selectedNoteDetail?.id,
                    onSelectNote = { viewModel.selectedNoteDetail.value = it }
                )
            }

            Box(modifier = Modifier.weight(0.66f).fillMaxHeight().padding(8.dp)) {
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
        Column(modifier = Modifier.fillMaxSize().gridPaper(colors.darkBg, colors.rule).padding(12.dp)) {
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
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.fetchNotes(resetPage = true) }) {
                        StationeryIcon(name = "search", size = 18.dp, tint = colors.neonBlue)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.cardBg,
                    unfocusedContainerColor = colors.cardBg,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.neonBlue,
                    unfocusedBorderColor = colors.edge
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedIconButton(
                onClick = { showFilters = !showFilters },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (showFilters) colors.neonBlue else colors.edge),
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = if (showFilters) colors.neonBlue.copy(alpha = 0.14f) else colors.cardBg
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StationeryIcon(name = "folder", size = 18.dp, tint = colors.textSecondary)
                    Text("暂无符合条件的错题笔记", color = colors.textSecondary)
                }
            }
        } else {
            val cardContext = LocalContext.current
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(notes) { note ->
                    NoteListItemCard(
                        note = note,
                        isSelected = note.id == selectedNoteId,
                        cardContext = cardContext,
                        onSelect = { onSelectNote(note) }
                    )
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
                        Text("上一页", fontSize = 13.5.sp)
                    }

                    Text(
                        text = "第 $currentPage / $totalPages 页 (共 $totalItems 条)",
                        fontSize = 13.5.sp,
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
                        Text("下一页", fontSize = 13.5.sp)
                    }
                }
            }
        }
    }
}
