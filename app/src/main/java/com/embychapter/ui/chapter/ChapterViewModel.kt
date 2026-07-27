package com.embychapter.ui.chapter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.embychapter.data.model.Chapter
import com.embychapter.data.model.SessionsResponse
import com.embychapter.data.repository.EmbyRepository
import com.embychapter.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChapterUiState(
    val serverUrl: String = "",
    val token: String = "",
    val username: String = "",
    val password: String = "",
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,

    // Current playing session
    val currentSession: SessionInfo? = null,
    val chapters: List<ChapterItem> = emptyList(),
    val newChapterName: String = "",
    val selectedCount: Int = 0,
    val isAllSelected: Boolean = false,

    // Delete confirmation dialog
    val showDeleteConfirm: Boolean = false
)

data class SessionInfo(
    val id: String,
    val title: String,
    val deviceName: String,
    val timeStr: String,
    val imageUrl: String?
)

data class ChapterItem(
    val name: String,
    val displayTime: String,
    val fullTime: String,
    val apiIndex: Int,
    val selected: Boolean = false
)

class ChapterViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val embyRepo = EmbyRepository()

    private val _uiState = MutableStateFlow(ChapterUiState())
    val uiState: StateFlow<ChapterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val serverUrl = settingsRepo.serverUrl.first()
            val token = settingsRepo.token.first()
            if (serverUrl.isNotBlank() && token.isNotBlank()) {
                _uiState.update {
                    it.copy(serverUrl = serverUrl, token = token, isLoggedIn = true)
                }
                // 恢复登录态时必须先初始化 apiService，否则 requireApi() 会抛 IllegalStateException
                embyRepo.setBaseUrl(serverUrl)
                fetchSessions()
            }
        }
    }

    fun updateServerUrl(url: String) { _uiState.update { it.copy(serverUrl = url) } }
    fun updateUsername(name: String) { _uiState.update { it.copy(username = name) } }
    fun updatePassword(pw: String) { _uiState.update { it.copy(password = pw) } }
    fun updateNewChapterName(name: String) { _uiState.update { it.copy(newChapterName = name) } }

    fun logout() {
        viewModelScope.launch {
            settingsRepo.clearAll()
            _uiState.update { ChapterUiState() }
        }
    }

    private var fetchSessionsJob: kotlinx.coroutines.Job? = null

    fun fetchSessions() {
        val state = _uiState.value
        // Guard: don't call API without login
        if (!state.isLoggedIn || state.token.isBlank()) {
            _uiState.update { it.copy(isError = true, statusMessage = "请先登录") }
            return
        }
        fetchSessionsJob?.cancel()
        fetchSessionsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null, isError = false) }
            embyRepo.getSessions(state.token).fold(
                onSuccess = { sessions ->
                    val playingSession = sessions.find { s ->
                        s.nowPlayingItem?.mediaType == "Video"
                    }
                    if (playingSession != null) {
                        val item = playingSession.nowPlayingItem!!
                        val ticks = playingSession.playState?.positionTicks ?: 0
                        val serverUrl = state.serverUrl.trim().trimEnd('/')
                        val imgTag = item.primaryImageTag ?: item.imageTags?.primary
                        val imgUrl = if (imgTag != null) "$serverUrl/emby/Items/${item.id}/Images/Primary?maxHeight=200&tag=$imgTag&api_key=${state.token}" else null

                        _uiState.update {
                            // Detect itemId change inside update block using latest state
                            val oldItemId = it.currentSession?.id
                            val shouldClearChapters = oldItemId != null && oldItemId != item.id
                            it.copy(
                                currentSession = SessionInfo(
                                    id = item.id,
                                    title = item.name ?: "",
                                    deviceName = playingSession.deviceName ?: "",
                                    timeStr = ticksToTimeStr(ticks),
                                    imageUrl = imgUrl
                                ),
                                chapters = if (shouldClearChapters) emptyList() else it.chapters,
                                isLoading = false,
                                isError = false,
                                statusMessage = null
                            )
                        }
                        fetchChapters(item.id)
                    } else {
                        _uiState.update {
                            it.copy(currentSession = null, chapters = emptyList(), isLoading = false, isError = false)
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, isError = true, statusMessage = e.message)
                    }
                }
            )
        }
    }

    private fun fetchChapters(itemId: String) {
        val state = _uiState.value
        viewModelScope.launch {
            embyRepo.getChapters(state.token, itemId).fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            chapters = response.chapters.map { c ->
                                ChapterItem(
                                    name = c.name,
                                    displayTime = c.startTime.split(".").first(),
                                    fullTime = c.startTime,
                                    apiIndex = c.index
                                )
                            },
                            isLoading = false,
                            isError = false,
                            statusMessage = null
                        )
                    }
                },
                onFailure = { e ->
                    // Show error but preserve existing chapters (don't clear on failure)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isError = true,
                            statusMessage = "获取章节失败: ${e.message ?: "未知错误"}"
                        )
                    }
                }
            )
        }
    }

    fun login() {
        val state = _uiState.value
        // 前端表单校验
        if (state.serverUrl.isBlank()) {
            _uiState.update { it.copy(isError = true, statusMessage = "请填写服务器地址") }
            return
        }
        if (state.username.isBlank()) {
            _uiState.update { it.copy(isError = true, statusMessage = "请填写用户名") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(isError = true, statusMessage = "请填写密码") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            embyRepo.authenticate(state.serverUrl, state.username, state.password).fold(
                onSuccess = { auth ->
                    val normalizedUrl = state.serverUrl.trim()
                    settingsRepo.setServerUrl(normalizedUrl)
                    settingsRepo.setToken(auth.accessToken)
                    settingsRepo.setUser(auth.user.id, auth.user.name)
                    _uiState.update {
                        it.copy(
                            serverUrl = normalizedUrl,
                            token = auth.accessToken,
                            isLoggedIn = true,
                            isLoading = false
                        )
                    }
                    fetchSessions()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isError = true,
                            statusMessage = e.message ?: "登录失败"
                        )
                    }
                }
            )
        }
    }

    fun addChapter() {
        val state = _uiState.value
        val session = state.currentSession ?: return
        val fullTime = session.timeStr
        val cleanTime = session.timeStr.split(".").first()
        val name = state.newChapterName.ifBlank { "Chapter $cleanTime" }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            embyRepo.addChapter(state.token, session.id, name, fullTime).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, newChapterName = "") }
                    // 延时刷新，等待服务器写入完成
                    kotlinx.coroutines.delay(800)
                    // Check if session is still the same video before refreshing
                    val currentId = _uiState.value.currentSession?.id
                    if (currentId == session.id) {
                        fetchChapters(session.id)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, isError = true, statusMessage = e.message)
                    }
                }
            )
        }
    }

    // Show delete confirmation dialog
    fun requestDelete() {
        val state = _uiState.value
        if (state.selectedCount == 0) return
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun confirmDelete() {
        val state = _uiState.value
        val session = state.currentSession ?: return
        val selectedIndices = state.chapters.filter { it.selected }.map { it.apiIndex }
        if (selectedIndices.isEmpty()) {
            _uiState.update { it.copy(showDeleteConfirm = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteConfirm = false) }
            embyRepo.deleteChapters(state.token, session.id, selectedIndices.joinToString(",")).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    // 延时刷新，等待服务器删除完成
                    kotlinx.coroutines.delay(800)
                    // Check if session is still the same video before refreshing
                    val currentId = _uiState.value.currentSession?.id
                    if (currentId == session.id) {
                        fetchChapters(session.id)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, isError = true, statusMessage = e.message)
                    }
                }
            )
        }
    }

    fun toggleItem(index: Int) {
        _uiState.update {
            if (index < 0 || index >= it.chapters.size) return@update it
            val newChapters = it.chapters.toMutableList()
            newChapters[index] = newChapters[index].copy(selected = !newChapters[index].selected)
            it.copy(
                chapters = newChapters,
                selectedCount = newChapters.count { c -> c.selected },
                isAllSelected = newChapters.isNotEmpty() && newChapters.all { c -> c.selected }
            )
        }
    }

    fun toggleSelectAll() {
        _uiState.update {
            if (it.chapters.isEmpty()) return@update it
            val newValue = !it.isAllSelected
            it.copy(
                chapters = it.chapters.map { c -> c.copy(selected = newValue) },
                selectedCount = if (newValue) it.chapters.size else 0,
                isAllSelected = newValue
            )
        }
    }

    fun clearStatus() { _uiState.update { it.copy(statusMessage = null, isError = false) } }

    companion object {
        fun ticksToTimeStr(ticks: Long): String {
            val seconds = ticks / 10_000_000.0
            val h = (seconds / 3600).toInt()
            val m = ((seconds % 3600) / 60).toInt()
            val s = (seconds % 60).toInt()
            val ms = ((seconds % 1) * 1000).toInt()
            return "%02d:%02d:%02d.%03d".format(h, m, s, ms)
        }
    }
}
