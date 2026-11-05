package com.sproutmemo.android.ui.book

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.model.QuestionBookDto
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun BookDeleteDialog(
    book: QuestionBookDto,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.neonRed)
            ) {
                Text("确认删除", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = { Text("确认删除该习题册？", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Text(
                text = "确定删除习题册 \"${book.name}\" 吗？\n删除后，该习题册下的所有错题关联都将解除。请确认是否删除该习题册。",
                color = colors.textSecondary
            )
        }
    )
}

@Composable
fun BookRenameDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
            ) {
                Text("保存修改", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = { Text("修改习题册名称", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Column {
                Text("请输入习题册新名称:", fontSize = 14.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.neonBlue,
                        unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.3f)
                    )
                )
            }
        }
    )
}
