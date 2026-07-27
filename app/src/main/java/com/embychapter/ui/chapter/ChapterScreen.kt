package com.embychapter.ui.chapter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.embychapter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(viewModel: ChapterViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero section
            item {
                HeroSection(
                    isLoggedIn = state.isLoggedIn,
                    hasSession = state.currentSession != null,
                    onRefresh = { viewModel.fetchSessions() },
                    onLogout = { viewModel.logout() }
                )
            }

            // Login form (if not logged in)
            if (!state.isLoggedIn) {
                item { LoginForm(state, viewModel) }
            }

            // Overview stats bar (only when logged in)
            if (state.isLoggedIn) {
                item { OverviewStatsBar(state) }
            }

            // Quick actions + chapter list (if logged in and playing)
            if (state.isLoggedIn && state.currentSession != null) {
                item { NowPlayingCard(state) }
                item { Spacer(Modifier.height(4.dp)) }
                item { QuickActionCard(state, viewModel) }
                item { Spacer(Modifier.height(4.dp)) }
                item { ChapterListCard(state, viewModel) }
            } else if (state.isLoggedIn) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardMint.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("未检测到正在播放的视频", style = MaterialTheme.typography.titleMedium)
                            Text("请先在电脑/电视播放，再点击刷新", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Delete confirmation dialog
    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${state.selectedCount} 个章节吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Danger)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun HeroSection(isLoggedIn: Boolean, hasSession: Boolean, onRefresh: () -> Unit, onLogout: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrimaryVariant.copy(alpha = 0.2f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Emby Chapter Console", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text("章节管理大师 Pro", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("把常用操作集中到顶部，进入页面就能直接刷新、加章节、查看历史。", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(top = 8.dp))

            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLoggedIn) {
                    AssistChip(
                        onClick = onRefresh,
                        label = { Text("刷新") },
                        leadingIcon = { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = onLogout,
                        label = { Text("退出", color = Danger) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(16.dp), tint = Danger) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewStatsBar(state: ChapterUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiniStatCard("登录", "已登录", CardMint, Modifier.weight(1f))
        MiniStatCard("章节", "${state.chapters.size}", CardCoral, Modifier.weight(1f))
        MiniStatCard("已选", "${state.selectedCount}", CardSky, Modifier.weight(1f))
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.25f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NowPlayingCard(state: ChapterUiState) {
    val session = state.currentSession ?: return
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Poster image
            if (session.imageUrl != null) {
                AsyncImage(
                    model = session.imageUrl,
                    contentDescription = session.title,
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(session.title.take(1), style = MaterialTheme.typography.headlineMedium, color = TextSecondary)
                }
            }

            // Playing info
            Column(modifier = Modifier.weight(1f)) {
                Text("正在播放", style = MaterialTheme.typography.labelSmall, color = Primary)
                Text(session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text("设备: ${session.deviceName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("进度: ${session.timeStr}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = Success.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "PLAYING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Success
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginForm(state: ChapterUiState, viewModel: ChapterViewModel) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("连接 Emby 服务器", style = MaterialTheme.typography.titleLarge)
            Text("填写服务器地址与账号后即可同步当前播放状态", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.serverUrl, onValueChange = viewModel::updateServerUrl,
                label = { Text("服务器地址") }, placeholder = { Text("http://192.168.1.5:8096") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.username, onValueChange = viewModel::updateUsername,
                label = { Text("用户名") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.password, onValueChange = viewModel::updatePassword,
                label = { Text("密码") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::login, modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Login, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("连接服务器")
            }
        }
    }
}

@Composable
private fun QuickActionCard(state: ChapterUiState, viewModel: ChapterViewModel) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("快捷操作", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.newChapterName,
                    onValueChange = viewModel::updateNewChapterName,
                    label = { Text("章节名称（留空默认）") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = viewModel::addChapter, enabled = !state.isLoading) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加当前点")
                }
            }
        }
    }
}

@Composable
private fun ChapterListCard(state: ChapterUiState, viewModel: ChapterViewModel) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("章节列表 (${state.chapters.size})", style = MaterialTheme.typography.titleMedium)
                Text("已选 ${state.selectedCount}", style = MaterialTheme.typography.labelMedium, color = Warning)
            }

            if (state.chapters.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::toggleSelectAll) {
                        Text(if (state.isAllSelected) "取消全选" else "全选")
                    }
                    TextButton(onClick = viewModel::fetchSessions) { Text("刷新") }
                    if (state.selectedCount > 0) {
                        TextButton(onClick = viewModel::requestDelete) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Danger)
                            Spacer(Modifier.width(4.dp))
                            Text("删除选中", color = Danger)
                        }
                    }
                }

                state.chapters.forEachIndexed { index, chapter ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.toggleItem(index) },
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = chapter.selected,
                            onCheckedChange = null
                        )
                        Column {
                            Text(chapter.displayTime, style = MaterialTheme.typography.labelLarge, color = Primary)
                            Text(chapter.name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("ID:${chapter.apiIndex}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    if (index < state.chapters.size - 1) {
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.1f))
                    }
                }
            } else {
                Text("该视频暂无章节信息", style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
