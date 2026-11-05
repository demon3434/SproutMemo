package com.sproutmemo.android.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.ui.components.ZoomableImageDialog
import com.sproutmemo.android.ui.components.VideoPlayerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ReviewPracticeScreen(
    viewModel: ReviewViewModel,
    sessionId: String,
    isTabletLandscape: Boolean,
    onBack: () -> Unit
) {
    val reviewState by viewModel.reviewState.collectAsState()
    val loading by viewModel.loading.collectAsState()

    // Extract current practicing session details
    val session = remember(reviewState) {
        when (val state = reviewState) {
            is ReviewState.Practicing -> state.session
            is ReviewState.Completed -> state.session
            else -> null
        }
    }

    val currentIndex = remember(reviewState) {
        when (val state = reviewState) {
            is ReviewState.Practicing -> state.currentIndex
            else -> 0
        }
    }

    LaunchedEffect(sessionId) {
        viewModel.loadSessionForPractice(sessionId)
    }

    if (session == null) {
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val currentNote = session.notes.orEmpty().getOrNull(currentIndex)

    // Form inputs state
    var selectedOutcome by remember { mutableStateOf("") }
    var newMastery by remember { mutableStateOf("PARTIAL") }
    var newStars by remember { mutableStateOf(1.0) }
    var excludeFromReview by remember { mutableStateOf(false) }

    // Initialize/Reset form inputs when question index or note changes
    LaunchedEffect(currentIndex, currentNote?.id) {
        if (currentNote != null) {
            newMastery = currentNote.mastery_level
            newStars = currentNote.difficulty_stars.toDouble()
            excludeFromReview = currentNote.exclude_from_review

            if (currentNote.is_completed && currentNote.review_log != null) {
                // If already completed in history or previous attempt, prefill outcome
                selectedOutcome = currentNote.review_log.outcome_code
                newMastery = currentNote.review_log.new_mastery
                newStars = currentNote.review_log.new_stars
            } else {
                selectedOutcome = ""
            }
        }
    }

    // Timer seconds state
    var seconds by remember { mutableStateOf(0L) }
    var isTimerActive by remember { mutableStateOf(true) }

    // Coroutine timer
    LaunchedEffect(currentIndex, isTimerActive) {
        seconds = 0L
        if (isTimerActive) {
            while (isActive) {
                delay(1000)
                seconds++
            }
        }
    }

    // Lifecycle observer to pause timer when app is backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                isTimerActive = false
            } else if (event == Lifecycle.Event.ON_RESUME) {
                isTimerActive = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Media preview dialog states
    var activeImageUrl by remember { mutableStateOf<String?>(null) }
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }

    // Confirmation dialog states
    var showAbandonDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    // Intercept hardware and gesture backs
    BackHandler(enabled = true) {
        showAbandonDialog = true
    }

    // Dialog 1: Abandon Confirmation
    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("⚠️ 确定放弃本次复习吗？") },
            text = { Text("放弃后，您在本次会话中的所有答题进度与打卡记录将不会保存。") },
            confirmButton = {
                Button(
                    onClick = {
                        showAbandonDialog = false
                        viewModel.abandonSession(sessionId)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认放弃")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAbandonDialog = false }) {
                    Text("继续答题")
                }
            }
        )
    }

    // Dialog 2: Submit Confirmation
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("🏁 确定提交并结束复习吗？") },
            text = { Text("本次复习共 ${session.target_count} 题，您已全部打卡完毕。点击确定后，成绩将同步到历史统计看板中。") },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        if (currentNote != null) {
                            viewModel.submitAnswer(
                                session = session,
                                noteId = currentNote.id,
                                outcomeCode = selectedOutcome,
                                newMastery = newMastery,
                                newStars = newStars,
                                excludeFromReview = excludeFromReview,
                                durationSeconds = seconds.toDouble(),
                                sequenceIndex = currentIndex + 1,
                                onSuccess = {
                                    onBack() // Back to DailyReviewScreen which will display Completed state
                                }
                            )
                        }
                    }
                ) {
                    Text("确认提交")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSubmitDialog = false }) {
                    Text("我再看看")
                }
            }
        )
    }

    // Media Overlays
    activeImageUrl?.let { url ->
        val matchedAttId = currentNote?.attachments?.find { att ->
            url.endsWith(att.storage_path)
        }?.id
        ZoomableImageDialog(
            imageUrl = url,
            noteId = currentNote?.id,
            attachmentId = matchedAttId,
            onNoteUpdated = { viewModel.loadSessionForPractice(sessionId) },
            onDismiss = { activeImageUrl = null }
        )
    }
    activeVideoUrl?.let { url ->
        VideoPlayerDialog(videoUrl = url, onDismiss = { activeVideoUrl = null })
    }

    fun formatTime(sec: Long): String {
        val m = sec / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }

    val onFormSubmit = {
        if (currentNote != null) {
            val isLast = currentIndex >= session.target_count - 1
            if (isLast && !currentNote.is_completed) {
                // If it is the last question and we are submitting for the first time, show confirmation dialog
                showSubmitDialog = true
            } else {
                viewModel.submitAnswer(
                    session = session,
                    noteId = currentNote.id,
                    outcomeCode = selectedOutcome,
                    newMastery = newMastery,
                    newStars = newStars,
                    excludeFromReview = excludeFromReview,
                    durationSeconds = seconds.toDouble(),
                    sequenceIndex = currentIndex + 1,
                    onSuccess = {
                        if (isLast) {
                            onBack() // Back to DailyReviewScreen for Completed screen
                        }
                    }
                )
            }
        }
    }

    if (isTabletLandscape) {
        ReviewPracticeTabletContent(
            session = session,
            currentIndex = currentIndex,
            onSelectNote = { viewModel.setPracticeIndex(session, it) },
            selectedOutcome = selectedOutcome,
            onSelectOutcome = { selectedOutcome = it },
            newMastery = newMastery,
            onMasteryChange = { newMastery = it },
            newStars = newStars,
            onStarsChange = { newStars = it },
            excludeFromReview = excludeFromReview,
            onExcludeChange = { excludeFromReview = it },
            submitting = loading,
            onSubmit = onFormSubmit,
            timerText = formatTime(seconds),
            onOpenImage = { activeImageUrl = it },
            onOpenVideo = { activeVideoUrl = it },
            onBackClick = { showAbandonDialog = true }
        )
    } else {
        ReviewPracticePhoneContent(
            session = session,
            currentIndex = currentIndex,
            onSelectNote = { viewModel.setPracticeIndex(session, it) },
            selectedOutcome = selectedOutcome,
            onSelectOutcome = { selectedOutcome = it },
            newMastery = newMastery,
            onMasteryChange = { newMastery = it },
            newStars = newStars,
            onStarsChange = { newStars = it },
            excludeFromReview = excludeFromReview,
            onExcludeChange = { excludeFromReview = it },
            submitting = loading,
            onSubmit = onFormSubmit,
            timerText = formatTime(seconds),
            onOpenImage = { activeImageUrl = it },
            onOpenVideo = { activeVideoUrl = it },
            onBackClick = { showAbandonDialog = true }
        )
    }
}
