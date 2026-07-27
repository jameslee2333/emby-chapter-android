package com.embychapter.ui.videowall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.embychapter.data.model.EmbyItem
import com.embychapter.data.repository.EmbyRepository
import com.embychapter.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VideoItem(
    val id: String,
    val title: String,
    val originalTitle: String = "",
    val year: String = "",
    val type: String = "Movie",
    val typeLabel: String = "电影",
    val overview: String = "",
    val imageTag: String = "",
    val genreLabel: String = "",
    val posterGradient: String = "",
    val imageUrl: String = "",
    val itemUrl: String = ""
)

data class VideoWallUiState(
    val serverUrl: String = "",
    val token: String = "",
    val isLoading: Boolean = false,
    val isUsingCache: Boolean = false,
    val statusMessage: String? = null,
    val searchKeyword: String = "",
    val videos: List<VideoItem> = emptyList(),
    val sortBy: SortType = SortType.RECENT,
    val selectedVideo: VideoItem? = null
)

enum class SortType { RECENT, TITLE }

class VideoWallViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val embyRepo = EmbyRepository()

    private val _uiState = MutableStateFlow(VideoWallUiState())
    val uiState: StateFlow<VideoWallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val url = settingsRepo.serverUrl.first()
            val token = settingsRepo.token.first()
            _uiState.update { it.copy(serverUrl = url, token = token) }
        }
    }

    fun updateSearchKeyword(kw: String) { _uiState.update { it.copy(searchKeyword = kw) } }
    fun setSortBy(sort: SortType) { _uiState.update { it.copy(sortBy = sort) } }
    fun selectVideo(item: VideoItem?) { _uiState.update { it.copy(selectedVideo = item) } }

    fun refreshVideos() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            embyRepo.setBaseUrl(state.serverUrl)
            embyRepo.getItems(state.token).fold(
                onSuccess = { items ->
                    val videos = items.map { normalizeItem(it, state.serverUrl, state.token) }
                    _uiState.update {
                        it.copy(videos = videos, isLoading = false, isUsingCache = false)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, statusMessage = e.message)
                    }
                }
            )
        }
    }

    val filteredVideos: List<VideoItem> get() {
        val state = _uiState.value
        val kw = state.searchKeyword.trim().lowercase()
        var list = state.videos.filter {
            kw.isEmpty() || it.title.lowercase().contains(kw) || it.originalTitle.lowercase().contains(kw)
        }
        list = when (state.sortBy) {
            SortType.TITLE -> list.sortedBy { it.title }
            SortType.RECENT -> list.sortedByDescending { it.year.toIntOrNull() ?: 0 }
        }
        return list
    }

    private fun normalizeItem(item: EmbyItem, serverUrl: String, token: String): VideoItem {
        val base = serverUrl.trimEnd('/')
        val imageTag = item.primaryImageTag ?: item.imageTags?.primary ?: ""
        val genreLabel = item.genres?.take(3)?.joinToString(" / ") ?: ""
        return VideoItem(
            id = item.id,
            title = item.name ?: "",
            originalTitle = item.originalTitle ?: "",
            year = "${item.productionYear ?: ""}",
            type = item.type ?: "Movie",
            typeLabel = getTypeLabel(item.type),
            overview = item.overview ?: "",
            imageTag = imageTag,
            genreLabel = genreLabel,
            imageUrl = if (imageTag.isNotEmpty()) "$base/emby/Items/${item.id}/Images/Primary?maxWidth=360&quality=90&tag=$imageTag&api_key=$token" else "",
            itemUrl = "$base/web/index.html#!/item?id=${item.id}"
        )
    }

    private fun getTypeLabel(type: String?): String = when (type) {
        "Movie" -> "电影"
        "Series" -> "剧集"
        "Episode" -> "剧集单集"
        else -> type ?: "影片"
    }
}
