package com.embychapter.ui.videowall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.embychapter.ui.theme.*

@Composable
fun VideoWallScreen(viewModel: VideoWallViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val videos = viewModel.pagedVideos
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Hero
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.18f)),
            shape = ExpressiveShapes.HeroCard
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Emby Video Wall",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    "离线也能看的视频海报墙",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "先展示本地缓存，再尝试同步服务器影片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                HorizontalFloatingToolbar(
                    expanded = true,
                    modifier = Modifier.padding(top = 16.dp),
                    content = {
                        Button(
                            onClick = { viewModel.refreshVideos() },
                            enabled = !state.isLoading
                        ) {
                            if (state.isLoading) CircularProgressIndicator(
                                Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            else Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("刷新影片")
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Status strip
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("数据源", if (state.isUsingCache) "本地缓存" else "服务器", CardCoral, AccentCoralSoft, Modifier.weight(1f))
            StatCard("影片数", "${viewModel.filteredVideos.size}", CardMint, AccentTealSoft, Modifier.weight(1f))
            StatCard("上次同步", viewModel.lastSyncLabel, CardSky, AccentAmberSoft, Modifier.weight(1f))
        }

        // Status message bar
        state.statusMessage?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Surface(
                color = SurfaceLight,
                shape = ExpressiveShapes.Pill,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Search & Sort
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchKeyword,
                onValueChange = viewModel::updateSearchKeyword,
                label = { Text("搜索标题") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.searchKeyword.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchKeyword("") }) {
                            Icon(Icons.Default.Clear, "清空搜索")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = ExpressiveShapes.Pill
            )
            IconButton(
                onClick = {
                    val new = if (state.sortBy == SortType.RECENT) SortType.TITLE else SortType.RECENT
                    viewModel.setSortBy(new)
                },
                modifier = Modifier.size(48.dp)
            ) { Icon(Icons.Default.Sort, state.sortBy.name) }
        }

        Spacer(Modifier.height(12.dp))

        // Video grid
        if (state.isLoading && videos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (videos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    viewModel.emptyStateMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(videos, key = { it.id }) { item ->
                    Card(
                        Modifier.aspectRatio(0.7f).clickable { viewModel.selectVideo(item) },
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = ExpressiveShapes.SquircleLarge
                    ) {
                        Column {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Gradient fallback based on title hash
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(ExpressiveShapes.SquircleLarge)
                                            .background(posterColor(item.id)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            item.title.take(1),
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                                if (item.year.isNotBlank()) {
                                    Surface(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        shape = ExpressiveShapes.Pill
                                    ) {
                                        Text(
                                            item.year,
                                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Text(
                                item.title,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Pagination
            if (viewModel.showPagination) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            ) {
                                Text(
                                    "$page",
                                    fontWeight = if (page == state.currentPage) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        TextButton(
                            onClick = { viewModel.changePage(state.currentPage + 1) },
                            enabled = state.currentPage < viewModel.totalPages
                        ) { Text("下一页") }
                    }
                }
            }
        }

        // Detail modal
        state.selectedVideo?.let { video ->
            AlertDialog(
                onDismissRequest = { viewModel.selectVideo(null) },
                title = { Text(video.title) },
                text = {
                    Column {
                        DetailRow("年份", video.year)
                        DetailRow("类型", video.typeLabel)
                        DetailRow("原始标题", video.originalTitle.ifBlank { "-" })
                        DetailRow("分类", video.genreLabel.ifBlank { "-" })
                        DetailRow("简介", video.overview.ifBlank { "暂无简介" })
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("item_url", video.itemUrl))
                        }) { Text("复制地址") }
                        TextButton(onClick = { viewModel.selectVideo(null) }) { Text("关闭") }
                    }
                }
            )
        }
    }
}

private fun posterColor(id: String): Color {
    var hash = 0
    for (c in id) hash = c.code + ((hash shl 5) - hash)
    val hue = (Math.abs(hash) % 360).toFloat()
    val hsv = floatArrayOf(hue, 0.5f, 0.4f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun StatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = ExpressiveShapes.StatCard
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
