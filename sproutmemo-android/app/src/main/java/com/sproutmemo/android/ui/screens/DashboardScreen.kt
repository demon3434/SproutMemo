package com.sproutmemo.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCreateNoteClick: () -> Unit,
    onLogout: () -> Unit
) {
    val mockNotes = listOf(
        "数学 - 第一单元错题 (难度 ★★★)",
        "语文 - 第二课拼音练习 (难度 ★★)",
        "英语 - 单词听写拼写错误 (难度 ★★★★)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("错题本主页") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "退出登录")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNoteClick) {
                Icon(Icons.Default.Add, contentDescription = "新建错题")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "难题错题列表",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(mockNotes) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(note, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
