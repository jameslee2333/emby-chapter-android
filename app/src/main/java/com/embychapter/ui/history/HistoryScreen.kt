package com.embychapter.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.embychapter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Secondary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Emby Insight Deck", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text("播放历史、继续观看与最近记录", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.refreshAll() }, enabled = !state.isLoading) {
                            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("刷新全部")
                        }
                    }
                }
            }
        }

        // Config
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = state.apiBaseUrl, onValueChange = viewModel::updateApiBaseUrl,
                        label = { Text("历史服务地址") }, placeholder = { Text("http://192.168.1.8:3000") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.saveConfig(); viewModel.refreshAll() }) { Text("保存并刷新") }
                        OutlinedButton(onClick = { viewModel.updateHistoryUserId(""); viewModel.saveConfig() }) { Text("清除过滤") }
                    }
                }
            }
        }

        // Stats cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("总播放", "${state.stats.totalPlays}", CardCoral, Modifier.weight(1f))
                StatCard("完成", "${state.stats.finishedPlays}", CardMint, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("总时长", HistoryViewModel.formatDuration(state.stats.totalDuration), CardSky, Modifier.weight(1f))
                StatCard("实际观看", HistoryViewModel.formatDuration(state.stats.actualTotalDuration), CardAmber, Modifier.weight(1f))
            }
        }

        // Section tabs
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionType.entries.forEach { type ->
                    FilterChip(
                        selected = state.currentSection == type,
                        onClick = { viewModel.switchSection(type) },
                        label = { Text(type.name) }
                    )
                }
            }
        }

        // Content
        val data = viewModel.filteredData
        if (state.error != null) {
            item { Text("错误: ${state.error}", color = Danger, modifier = Modifier.padding(8.dp)) }
        } else if (data.isEmpty() && !state.isLoading) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceLight)) {
                    Text("暂无数据", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            itemsIndexed(data) { index, item ->
                Card(
                    Modifier.fillMaxWidth().clickable { },
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.title ?: "未命名", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.type ?: "-", style = MaterialTheme.typography.labelSmall, color = Primary)
                            Text(item.userName ?: "-", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(item.playedAt?.take(10) ?: item.lastPlayedAt?.take(10) ?: "-", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        if (item.progress != null) {
                            LinearProgressIndicator(
                                progress = { item.progress / 100f },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                color = if ((item.isFinished ?: 0) == 1) Success else Warning,
                                trackColor = SurfaceLight
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.3f))) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}
