package com.embychapter.ui.videowall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.embychapter.data.model.EmbyItem
import com.embychapter.data.model.ItemsResponse
import com.embychapter.data.repository.EmbyRepository
import com.embychapter.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromString

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
    val hasServerConfig: Boolean = false,
    val lastSyncTime: Long = 0L,
    val statusMessage: String? = null,
    val searchKeyword: String = "",
    val videos: List<VideoItem> = emptyList(),
    val sortBy: SortType = SortType.RECENT,
    val selectedVideo: VideoItem? = null,
    val currentPage: Int = 1
)

enum class SortType { RECENT, TITLE }

class VideoWallViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val embyRepo = EmbyRepository()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private val _uiState = MutableStateFlow(VideoWallUiState())
    val uiState: StateFlow<VideoWallUiState> = _uiState.asStateFlow()

    companion object {
        const val ITEMS_PER_PAGE = 24
    }

    init {
        viewModelScope.launch {
            val url = settingsRepo.serverUrl.first()
            val token = settingsRepo.token.first()
            val sortPref = settingsRepo.sortPreference.first()
            val syncTime = settingsRepo.lastSyncTime.first()
            val sortBy = runCatching { SortType.valueOf(sortPref) }.getOrDefault(SortType.RECENT)

            _uiState.update {
                it.copy(
                    serverUrl = url, token = token,
                    hasServerConfig = url.isNotBlank() && token.isNotBlank(),
                    sortBy = sortBy,
                    lastSyncTime = syncTime
                )
            }

            // Load cache first for instant display
            loadCache()

            // Then try server sync if configured
            if (url.isNotBlank() && token.isNotBlank()) {
                refreshVideos()
            }
        }
    }

    fun updateSearchKeyword(kw: String) { _uiState.update { it.copy(searchKeyword = kw, currentPage = 1) } }

    fun setSortBy(sort: SortType) {
        _uiState.update { it.copy(sortBy = sort, currentPage = 1) }
        viewModelScope.launch { settingsRepo.setSortPreference(sort.name) }
    }

    fun selectVideo(item: VideoItem?) { _uiState.update { it.copy(selectedVideo = item) } }

    fun changePage(page: Int) {
        val total = totalPages
        if (page < 1 || page > total) return
        _uiState.update { it.copy(currentPage = page) }
    }

    // ── Cache operations ────────────────────────────────────

    private suspend fun loadCache() {
        val cachedJson = settingsRepo.cachedVideos.first()
        if (cachedJson.isNotBlank()) {
            try {
                val items = json.decodeFromString<List<EmbyItem>>(cachedJson)
                val state = _uiState.value
                val videos = items.map { normalizeItem(it, state.serverUrl, state.token) }
                _uiState.update {
                    it.copy(
                        videos = videos,
                        isUsingCache = true,
                        currentPage = 1,
                        statusMessage = "已加载本地缓存（${videos.size} 部影片）"
                    )
                }
            } catch (e: Exception) {
                // Cache parse failed, set status
                _uiState.update { it.copy(isUsingCache = false) }
            }
        }
    }

    private suspend fun saveCache(items: List<EmbyItem>) {
        try {
            val jsonString = json.encodeToString<List<EmbyItem>>(items)
            val now = System.currentTimeMillis()
            // Atomic write: both keys in a single DataStore edit
            settingsRepo.saveVideoCache(jsonString, now)
            _uiState.update { it.copy(lastSyncTime = now) }
        } catch (e: Exception) {
            // Cache save failed, non-critical
        }
    }

    // ── Server sync ─────────────────────────────────────────

    private var refreshJob: kotlinx.coroutines.Job? = null

    fun refreshVideos() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.token.isBlank()) {
            _uiState.update { it.copy(statusMessage = "请先在章节管理页配置并登录 Emby 服务器") }
            return
        }
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            embyRepo.setBaseUrl(state.serverUrl)
            embyRepo.getItems(state.token).fold(
                onSuccess = { items ->
                    val videos = items.map { normalizeItem(it, state.serverUrl, state.token) }
                    _uiState.update {
                        it.copy(
                            videos = videos,
                            isLoading = false,
                            isUsingCache = false,
                            currentPage = 1,
                            statusMessage = "已同步 ${videos.size} 部影片"
                        )
                    }
                    // Save to cache
                    saveCache(items)
                },
                onFailure = { e ->
                    // Fallback to cache if we already have data (regardless of source)
                    val hasData = _uiState.value.videos.isNotEmpty()
                    if (hasData) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                statusMessage = "服务器连接失败，已回退到本地缓存"
                            )
                        }
                    } else {
                        // No data in memory, try loading from storage
                        _uiState.update { it.copy(isLoading = false) }
                        loadCache()
                        val stillEmpty = _uiState.value.videos.isEmpty()
                        _uiState.update {
                            it.copy(
                                statusMessage = if (stillEmpty) "同步失败: ${e.message}" else "服务器连接失败，已加载本地缓存"
                            )
                        }
                    }
                }
            )
        }
    }

    // ── Computed data ────────────────────────────────────────

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

    val pagedVideos: List<VideoItem> get() {
        val state = _uiState.value
        val start = (state.currentPage - 1) * ITEMS_PER_PAGE
        if (start >= filteredVideos.size) return emptyList()
        return filteredVideos.subList(start, minOf(start + ITEMS_PER_PAGE, filteredVideos.size))
    }

    val totalPages: Int get() = maxOf(1, (filteredVideos.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)

    val showPagination: Boolean get() = totalPages > 1

    val visiblePages: List<Int> get() {
        val current = _uiState.value.currentPage
        val total = totalPages
        val maxVisible = 5
        var start = maxOf(1, current - 2)
        var end = minOf(total, start + maxVisible - 1)
        if (end - start < maxVisible - 1) start = maxOf(1, end - maxVisible + 1)
        return (start..end).toList()
    }

    // Empty state type for multi-scenario display
    enum class EmptyState { NOT_CONFIGURED, NO_MATCH, NO_CACHE, NOT_LOADED }
    val emptyState: EmptyState get() {
        val state = _uiState.value
        if (!state.hasServerConfig && state.videos.isEmpty()) return EmptyState.NOT_CONFIGURED
        if (state.searchKeyword.isNotBlank() && filteredVideos.isEmpty()) return EmptyState.NO_MATCH
        if (state.videos.isEmpty() && state.isUsingCache) return EmptyState.NO_CACHE
        return EmptyState.NOT_LOADED
    }

    val emptyStateMessage: String get() = when (emptyState) {
        EmptyState.NOT_CONFIGURED -> "请先在章节管理页配置并登录 Emby 服务器"
        EmptyState.NO_MATCH -> "没有匹配的影片"
        EmptyState.NO_CACHE -> "无本地缓存，请连接服务器同步"
        EmptyState.NOT_LOADED -> "暂无影片，点击刷新同步"
    }

    val lastSyncLabel: String get() {
        val time = _uiState.value.lastSyncTime
        if (time == 0L) return "未同步"
        val diff = System.currentTimeMillis() - time
        val minutes = diff / 60000
        return when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "${minutes} 分钟前"
            minutes < 1440 -> "${minutes / 60} 小时前"
            else -> "${minutes / 1440} 天前"
        }
    }

    // ── Normalization ────────────────────────────────────────

    private fun normalizeItem(item: EmbyItem, serverUrl: String, token: String): VideoItem {
        val base = serverUrl.trimEnd('/')
        val imageTag = item.primaryImageTag ?: item.imageTags?.primary ?: ""
        val genreLabel = item.genres?.take(3)?.joinToString(" / ") ?: ""
        val gradient = buildPosterGradient(item.id, item.name)
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
            posterGradient = gradient,
            imageUrl = if (imageTag.isNotEmpty()) "$base/emby/Items/${item.id}/Images/Primary?maxWidth=360&quality=90&tag=$imageTag&api_key=$token" else "",
            itemUrl = "$base/web/index.html#!/item?id=${item.id}"
        )
    }

    private fun getTypeLabel(type: String?): String = when (type) {
        "Movie" -> "电影"
        "Series" -> "剧集"
        "Episode" -> "剧集单集"
        "Video" -> "视频"
        else -> type ?: "影片"
    }

    private fun buildPosterGradient(id: String, title: String?): String {
        val seed = if (id.isNotEmpty()) id else (title ?: "emby")
        val hue1 = colorFromSeed(seed)
        val hue2 = colorFromSeed("${seed}-accent")
        return "linear-gradient(135deg, hsl($hue1, 68%, 62%) 0%, hsl($hue2, 68%, 62%) 100%)"
    }

    private fun colorFromSeed(seed: String): Int {
        var hash = 0
        for (c in seed) hash = c.code + ((hash shl 5) - hash)
        return Math.abs(hash) % 360
    }
}
