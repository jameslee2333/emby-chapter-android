package com.embychapter.ui.chapter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.embychapter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(viewModel: ChapterViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    // Handle snackbar
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

            // Quick actions (if logged in and playing)
            if (state.isLoggedIn && state.currentSession != null) {
                item {
                    QuickActionCard(state, viewModel)
                }
                item { Spacer(Modifier.height(8.dp)) }

                // Chapter list
                item {
                    ChapterListCard(state, viewModel)
                }
            } else if (state.isLoggedIn) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardMint.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text("未检测到正在播放的视频", style = MaterialTheme.typography.titleMedium)
                            Text("请先在电脑/电视播放，再点击刷新", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
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
                AssistChip(
                    onClick = onRefresh,
                    label = { Text("刷新") },
                    leadingIcon = { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp)) }
                )
                if (isLoggedIn) {
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
                        TextButton(onClick = viewModel::deleteSelectedChapters) {
                            Text("删除选中", color = Danger)
                        }
                    }
                }

                itemsIndexed(state.chapters) { index, chapter ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = chapter.selected,
                            onCheckedChange = { viewModel.toggleItem(index) }
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
