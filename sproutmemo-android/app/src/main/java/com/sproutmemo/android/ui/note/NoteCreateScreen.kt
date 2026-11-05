package com.sproutmemo.android.ui.note

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun NoteCreateScreen(
    viewModel: NoteCreateViewModel,
    onNoteCreated: () -> Unit
) {
    val context = LocalContext.current
    val submitState by viewModel.submitState.collectAsState()

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is CreateNoteState.Success -> {
                Toast.makeText(context, "习题笔记录入成功！", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onNoteCreated()
            }
            is CreateNoteState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    NoteFormContent(
        viewModel = viewModel,
        submitButtonText = "保存并录入习题",
        onCancel = onNoteCreated
    )
}
