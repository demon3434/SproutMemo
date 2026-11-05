package com.sproutmemo.android.ui.book

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun BookQuickAddCard(
    bookName: String,
    onBookNameChange: (String) -> Unit,
    onAdd: () -> Unit,
    addError: String?
) {
    val colors = LocalAppColors.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📚 录入新习题册", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("（将添加至当前学科）", fontSize = 11.sp, color = colors.textSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = bookName,
                    onValueChange = onBookNameChange,
                    placeholder = { Text("习题册完整名称，如：《数学口算题卡》", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.neonBlue,
                        unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = colors.neonBlue,
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                ) {
                    Text("＋ 录入", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (!addError.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = addError,
                    color = colors.neonRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
