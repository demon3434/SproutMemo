package com.sproutmemo.android.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun VideoQualitySettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val currentQuality by viewModel.videoQuality.collectAsState()

    val qualities = remember {
        listOf(
            Pair("360p", "极小体积 (360P) [适合简易草稿记录]"),
            Pair("480p", "标清流畅 (480P) [适合一般步骤演示]"),
            Pair("720p", "高清标准 (720P) [清晰省流最佳推荐]"),
            Pair("1080p", "超清画质 (1080P) [保留精细手写字迹]")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.darkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎬 视频上传压缩设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        qualities.forEach { (id, label) ->
            val isSelected = currentQuality == id
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) colors.neonBlue else colors.textSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        viewModel.selectVideoQuality(id)
                        Toast.makeText(context, "视频压缩品质已更新！", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.neonBlue else colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectVideoQuality(id)
                            Toast.makeText(context, "视频压缩品质已更新！", Toast.LENGTH_SHORT).show()
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.neonBlue,
                            unselectedColor = colors.textSecondary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}
