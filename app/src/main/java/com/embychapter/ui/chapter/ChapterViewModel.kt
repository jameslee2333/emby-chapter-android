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
    val isAllSelected: Boolean = false
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
                fetchSessions()
            }
        }
    }

    fun updateServerUrl(url: String) { _uiState.update { it.copy(serverUrl = url) } }
    fun updateUsername(name: String) { _uiState.update { it.copy(username = name) } }
    fun updatePassword(pw: String) { _uiState.update { it.copy(password = pw) } }
    fun updateNewChapterName(name: String) { _uiState.update { it.copy(newChapterName = name) } }

    fun login() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            embyRepo.authenticate(state.serverUrl, state.username, state.password).fold(
                onSuccess = { auth ->
                    settingsRepo.setServerUrl(state.serverUrl)
                    settingsRepo.setToken(auth.accessToken)
                    settingsRepo.setUser(auth.user.id, auth.user.name)
                    _uiState.update {
                        it.copy(
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

    fun logout() {
        viewModelScope.launch {
            settingsRepo.clearAll()
            _uiState.update { ChapterUiState() }
        }
    }

    fun fetchSessions() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            embyRepo.getSessions(state.token).fold(
                onSuccess = { sessions ->
                    val playingSession = sessions.find { s ->
                        s.nowPlayingItem?.mediaType == "Video"
                    }
                    if (playingSession != null) {
                        val item = playingSession.nowPlayingItem!!
                        val ticks = playingSession.playState?.positionTicks ?: 0
                        val serverUrl = state.serverUrl.trimEnd('/')
                        val imgTag = item.primaryImageTag ?: item.imageTags?.primary
                        val imgUrl = if (imgTag != null) "$serverUrl/Items/${item.id}/Images/Primary?maxHeight=200&tag=$imgTag&api_key=${state.token}" else null

                        _uiState.update {
                            it.copy(
                                currentSession = SessionInfo(
                                    id = item.id,
                                    title = item.name ?: "",
                                    deviceName = playingSession.deviceName ?: "",
                                    timeStr = ticksToTimeStr(ticks),
                                    imageUrl = imgUrl
                                ),
                                isLoading = false
                            )
                        }
                        fetchChapters(item.id)
                    } else {
                        _uiState.update {
                            it.copy(currentSession = null, chapters = emptyList(), isLoading = false)
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
                            isLoading = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(chapters = emptyList(), isLoading = false) }
                }
            )
        }
    }

    fun addChapter() {
        val state = _uiState.value
        val session = state.currentSession ?: return
        val cleanTime = session.timeStr
        val name = state.newChapterName.ifBlank { "Chapter $cleanTime" }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            embyRepo.addChapter(state.token, session.id, name, cleanTime).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, newChapterName = "") }
                    fetchChapters(session.id)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, isError = true, statusMessage = e.message)
                    }
                }
            )
        }
    }

    fun deleteSelectedChapters() {
        val state = _uiState.value
        val session = state.currentSession ?: return
        val selectedIndices = state.chapters.filter { it.selected }.map { it.apiIndex }
        if (selectedIndices.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            embyRepo.deleteChapters(state.token, session.id, selectedIndices.joinToString(",")).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    fetchChapters(session.id)
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
            val newChapters = it.chapters.toMutableList()
            newChapters[index] = newChapters[index].copy(selected = !newChapters[index].selected)
            it.copy(
                chapters = newChapters,
                selectedCount = newChapters.count { c -> c.selected },
                isAllSelected = newChapters.all { c -> c.selected }
            )
        }
    }

    fun toggleSelectAll() {
        _uiState.update {
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
