package com.sproutmemo.android.ui.components

import androidx.compose.runtime.Composable
import com.sproutmemo.android.data.remote.model.NoteDto

@Composable
fun ZoomableImageDialog(
    imageUrl: String,
    noteId: String? = null,
    attachmentId: String? = null,
    onNoteUpdated: ((NoteDto) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    NoteImageLightboxDialog(
        imageUrls = listOf(imageUrl),
        initialIndex = 0,
        noteId = noteId,
        attachmentIds = if (attachmentId != null) listOf(attachmentId) else emptyList(),
        onNoteUpdated = onNoteUpdated,
        onDismiss = onDismiss
    )
}
