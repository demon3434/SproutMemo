package com.sproutmemo.android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Form inputs state
    var subject by remember { mutableStateOf("MATH") }
    var unitName by remember { mutableStateOf("") }
    var pageNumber by remember { mutableStateOf("") }
    var auxiliaryText by remember { mutableStateOf("") }

    // Media lists (simulating paths of attached files)
    val attachedImages = remember { mutableStateListOf<String>() }
    val attachedAudios = remember { mutableStateListOf<String>() }
    val attachedVideos = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("录入错题难题") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subject Selection
            Text("选择科目", style = MaterialTheme.typography.titleMedium)
            val subjects = listOf("MATH", "CHINESE", "ENGLISH", "PHYSICS", "CHEMISTRY")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { sub ->
                    FilterChip(
                        selected = subject == sub,
                        onClick = { subject = sub },
                        label = { Text(sub) }
                    )
                }
            }

            // Input fields
            OutlinedTextField(
                value = unitName,
                onValueChange = { unitName = it },
                label = { Text("单元名称") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pageNumber,
                onValueChange = { pageNumber = it },
                label = { Text("所在页码") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = auxiliaryText,
                onValueChange = { auxiliaryText = it },
                label = { Text("辅助描述正文") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Divider()

            // Media attachments section
            Text("添加多媒体附件 (限制: 9张图片, 6个音频, 3个视频)", style = MaterialTheme.typography.titleMedium)

            // Images attachment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("图片 (${attachedImages.size}/9)")
                Button(
                    onClick = {
                        if (attachedImages.size >= 9) {
                            Toast.makeText(context, "图片数量不能超过9张", Toast.LENGTH_SHORT).show()
                        } else {
                            // Mock adding image file path
                            attachedImages.add("mock_image_${attachedImages.size + 1}.jpg")
                        }
                    }
                ) {
                    Text("+ 添加图片")
                }
            }

            // Audio attachment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("讲解音频 (${attachedAudios.size}/6)")
                Button(
                    onClick = {
                        if (attachedAudios.size >= 6) {
                            Toast.makeText(context, "音频文件不能超过6个", Toast.LENGTH_SHORT).show()
                        } else {
                            // Mock adding audio file path
                            attachedAudios.add("mock_audio_${attachedAudios.size + 1}.mp3")
                        }
                    }
                ) {
                    Text("+ 录制讲解")
                }
            }

            // Video attachment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("讲解视频 (${attachedVideos.size}/3)")
                Button(
                    onClick = {
                        if (attachedVideos.size >= 3) {
                            Toast.makeText(context, "视频文件不能超过3个", Toast.LENGTH_SHORT).show()
                        } else {
                            // Mock adding video file path
                            attachedVideos.add("mock_video_${attachedVideos.size + 1}.mp4")
                        }
                    }
                ) {
                    Text("+ 拍摄视频")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Note Button
            Button(
                onClick = {
                    Toast.makeText(context, "错题笔记已成功保存", Toast.LENGTH_LONG).show()
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存错题")
            }
        }
    }
}
