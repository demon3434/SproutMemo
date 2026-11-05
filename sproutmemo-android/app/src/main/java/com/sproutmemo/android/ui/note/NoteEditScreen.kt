package com.sproutmemo.android.ui.note

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.LocalAppColors
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler

@Composable
fun NoteEditScreen(
    viewModel: NoteEditViewModel,
    onNoteUpdated: () -> Unit,
    onCancel: () -> Unit,
    onRegisterBackHandler: ((() -> Unit)?) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val submitState by viewModel.submitState.collectAsState()

    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val handleBackPress = remember {
        {
            showExitConfirmDialog = true
        }
    }

    // Intercept Android hardware back / gesture navigation
    BackHandler(enabled = true, onBack = handleBackPress)

    // Register back handler to parent controller for custom top-bar back arrow
    DisposableEffect(Unit) {
        onRegisterBackHandler(handleBackPress)
        onDispose {
            onRegisterBackHandler(null)
        }
    }

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is CreateNoteState.Success -> {
                Toast.makeText(context, "习题笔记修改成功！", Toast.LENGTH_SHORT).show()
                onNoteUpdated()
            }
            is CreateNoteState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    NoteFormContent(
        viewModel = viewModel,
        submitButtonText = "保存修改",
        onCancel = handleBackPress,
        onSubmitClick = {
            showSaveConfirmDialog = true
        }
    )

    // Save Confirmation Dialog
    if (showSaveConfirmDialog) {
        Dialog(onDismissRequest = { showSaveConfirmDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.darkBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "保存确认",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "确认保存对该习题的修改吗？",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSaveConfirmDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                showSaveConfirmDialog = false
                                viewModel.submitNote()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                        ) {
                            Text("确认", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitConfirmDialog) {
        Dialog(onDismissRequest = { showExitConfirmDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.darkBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "退出确认",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "您有未保存的修改，请选择：",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showExitConfirmDialog = false
                                viewModel.submitNote()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                        ) {
                            Text("保存修改并退出", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                showExitConfirmDialog = false
                                onCancel()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red)
                        ) {
                            Text("放弃修改并退出", color = Color.Red)
                        }
                        TextButton(
                            onClick = { showExitConfirmDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("取消（返回编辑）", color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
