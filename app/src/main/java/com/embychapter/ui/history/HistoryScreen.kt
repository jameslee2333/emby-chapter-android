package com.embychapter.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.embychapter.data.model.PlayRecord
import com.embychapter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
                        OutlinedButton(onClick = { viewModel.refreshCurrentSection() }, enabled = !state.isLoading) {
                            if (state.isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("刷新当前")
                        }
                        Button(onClick = { viewModel.refreshAll() }, enabled = !state.isLoading) {
                            Text("刷新全部")
                        }
                    }
                }
            }
        }

        // Config panel
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = state.apiBaseUrl, onValueChange = viewModel::updateApiBaseUrl,
                        label = { Text("历史服务地址") }, placeholder = { Text("http://192.168.1.8:3000") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.historyUserId, onValueChange = viewModel::updateHistoryUserId,
                        label = { Text("用户 ID 过滤") }, placeholder = { Text("留空为全部用户") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.saveConfigAndRefresh() }) { Text("保存并刷新") }
                        OutlinedButton(onClick = { viewModel.clearUserFilter() }) { Text("清空用户过滤") }
                    }
                }
            }
        }

        // Stats cards with derived metrics
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("总播放", HistoryViewModel.formatCount(state.stats.totalPlays), CardCoral, Modifier.weight(1f))
                StatCard("完成", "${HistoryViewModel.formatCount(state.stats.finishedPlays)} (${viewModel.completionRate}%)", CardMint, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("总时长", HistoryViewModel.formatDuration(state.stats.totalDuration), CardSky, Modifier.weight(1f))
                StatCard("平均观看", viewModel.averageWatchDuration, CardAmber, Modifier.weight(1f))
            }
        }

        // Section tabs
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionType.entries.forEach { type ->
                    FilterChip(
                        selected = state.currentSection == type,
                        onClick = { viewModel.switchSection(type) },
                        label = { Text(when (type) {
                            SectionType.HISTORY -> "播放历史"
                            SectionType.CONTINUE -> "继续观看"
                            SectionType.RECENT -> "最近播放"
                            SectionType.STATS -> "统计数据"
                        }) }
                    )
                }
            }
        }

        // Section badge (filtered count)
        item {
            Text(viewModel.filteredCountLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }

        // Filter toolbar (only for history & recent sections)
        if (viewModel.supportsFilter) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.searchKeyword,
                            onValueChange = viewModel::updateSearchKeyword,
                            label = { Text("搜索标题或原始标题") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        // User filter dropdown
                        FilterDropdown(
                            label = viewModel.currentUserFilterLabel,
                            options = viewModel.userOptions,
                            onSelect = { viewModel.setUserFilter(if (it == "所有用户") "" else it) }
                        )
                        // Device filter (history section only)
                        if (state.currentSection == SectionType.HISTORY) {
                            FilterDropdown(
                                label = viewModel.currentDeviceFilterLabel,
                                options = viewModel.deviceOptions,
                                onSelect = { viewModel.setDeviceFilter(if (it == "所有设备") "" else it) }
                            )
                            FilterDropdown(
                                label = viewModel.currentTypeFilterLabel,
                                options = viewModel.typeOptions,
                                onSelect = { viewModel.setTypeFilter(if (it == "所有类型") "" else it) }
                            )
                        }
                    }
                }
            }
        }

        // Content
        val data = viewModel.pagedData
        if (state.error != null) {
            item { Text("错误: ${state.error}", color = Danger, modifier = Modifier.padding(8.dp)) }
        } else if (state.isLoading && data.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        } else if (data.isEmpty() && !state.isLoading) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceLight)) {
                    Text("暂无数据", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else if (!state.isLoading || data.isNotEmpty()) {
            // Table header (sortable, not for continue section)
            if (state.currentSection != SectionType.CONTINUE) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            SortHeader("标题", "title", viewModel, Modifier.weight(2.2f))
                            SortHeader("类型", "type", viewModel, Modifier.weight(1f))
                            SortHeader("用户", "user_name", viewModel, Modifier.weight(1f))
                            SortHeader("时间", "played_at", viewModel, Modifier.weight(1.2f))
                        }
                    }
                }
            }

            // Continue section uses card layout; others use table rows
            if (state.currentSection == SectionType.CONTINUE) {
                itemsIndexed(data) { _, item ->
                    ContinueCard(item, viewModel)
                }
            } else {
                itemsIndexed(data) { _, item ->
                    HistoryRowCard(item, viewModel)
                }
            }

            // Pagination
            if (viewModel.showPagination) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.changePage(state.currentPage - 1) },
                            enabled = state.currentPage > 1
                        ) { Text("上一页") }
                        viewModel.visiblePages.forEach { page ->
                            TextButton(
                                onClick = { viewModel.changePage(page) },
                                colors = if (page == state.currentPage)
                                    ButtonDefaults.textButtonColors(contentColor = Primary)
                                else ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                            ) { Text("$page", fontWeight = if (page == state.currentPage) FontWeight.Bold else FontWeight.Normal) }
                        }
                        TextButton(
                            onClick = { viewModel.changePage(state.currentPage + 1) },
                            enabled = state.currentPage < viewModel.totalPages
                        ) { Text("下一页") }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    // Detail dialog
    state.selectedRecord?.let { record ->
        DetailDialog(record, viewModel)
    }
}

@Composable
private fun SortHeader(label: String, field: String, viewModel: HistoryViewModel, modifier: Modifier) {
    Text(
        text = "$label${viewModel.sortIndicator(field)}",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = modifier.clickable { viewModel.handleSort(field) }
    )
}

@Composable
private fun FilterDropdown(label: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun HistoryRowCard(item: PlayRecord, viewModel: HistoryViewModel) {
    Card(
        Modifier.fillMaxWidth().clickable { viewModel.openDetail(item) },
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(2.2f)) {
                    Text(item.title ?: "未命名", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(item.originalTitle ?: "-", style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                }
                Text(HistoryViewModel.getTypeLabel(item.type), style = MaterialTheme.typography.labelSmall, color = Primary, modifier = Modifier.weight(1f))
                Text(item.userName ?: item.userId ?: "未知", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(HistoryViewModel.formatDate(item.playedAt ?: item.lastPlayedAt), style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    Text("${HistoryViewModel.formatPercent(item.progress)} · ${if ((item.isFinished ?: 0) == 1) "已完成" else "未完成"}", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(item: PlayRecord, viewModel: HistoryViewModel) {
    Card(
        Modifier.fillMaxWidth().clickable { viewModel.openDetail(item) },
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(item.title ?: "未命名条目", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(HistoryViewModel.formatDateTime(item.lastPlayedAt ?: item.playedAt), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(HistoryViewModel.formatPercent(item.progress), style = MaterialTheme.typography.labelMedium, color = Warning, modifier = Modifier.weight(1f))
            }
            if (item.progress != null) {
                LinearProgressIndicator(
                    progress = { (item.progress ?: 0) / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = if ((item.isFinished ?: 0) == 1) Success else Warning,
                    trackColor = SurfaceLight
                )
            }
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("已看 ${HistoryViewModel.formatDuration(item.position ?: 0)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("总长 ${HistoryViewModel.formatDuration(item.duration ?: 0)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

@Composable
private fun DetailDialog(record: PlayRecord, viewModel: HistoryViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.closeDetail() },
        title = { Text("记录详情") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DetailRow("标题", record.title ?: "-")
                DetailRow("原始标题", record.originalTitle ?: "-")
                DetailRow("类型", HistoryViewModel.getTypeLabel(record.type))
                DetailRow("用户", record.userName ?: record.userId ?: "-")
                DetailRow("设备", record.device ?: "-")
                DetailRow("客户端", record.client ?: "-")
                DetailRow("播放时间", HistoryViewModel.formatDateTime(record.playedAt ?: record.lastPlayedAt ?: record.eventTime))
                DetailRow("播放进度", HistoryViewModel.formatPercent(record.progress))
                DetailRow("播放位置", "${HistoryViewModel.formatDuration(record.position ?: 0)} / ${HistoryViewModel.formatDuration(record.duration ?: 0)}")
                DetailRow("实际观看时长", HistoryViewModel.formatDuration(record.actualDuration ?: 0))
                DetailRow("状态", if ((record.isFinished ?: 0) == 1) "已完成" else "未完成")
                DetailRow("事件类型", record.eventType ?: "-")
                DetailRow("播放会话", record.playSessionId ?: "-")
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.closeDetail() }) { Text("关闭") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

@Composable
fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.3f))) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
