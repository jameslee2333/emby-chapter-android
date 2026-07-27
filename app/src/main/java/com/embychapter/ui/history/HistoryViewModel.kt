package com.embychapter.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.embychapter.data.model.PlayRecord
import com.embychapter.data.model.StatsResponse
import com.embychapter.data.repository.HistoryRepository
import com.embychapter.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val apiBaseUrl: String = "",
    val historyUserId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,

    // Stats
    val stats: StatsResponse = StatsResponse(),

    // Section
    val currentSection: SectionType = SectionType.HISTORY,
    val historyData: List<PlayRecord> = emptyList(),
    val continueData: List<PlayRecord> = emptyList(),
    val recentData: List<PlayRecord> = emptyList(),

    // Filters
    val searchKeyword: String = "",
    val selectedUser: String = "",
    val selectedDevice: String = "",
    val selectedType: String = "",

    // Sorting
    val sortField: String = "played_at",
    val sortDirection: String = "desc",

    // Pagination
    val currentPage: Int = 1,
    val itemsPerPage: Int = 10,

    // Detail
    val selectedRecord: PlayRecord? = null
)

enum class SectionType { HISTORY, CONTINUE, RECENT, STATS }

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val historyRepo = HistoryRepository()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedUrl = settingsRepo.historyServiceUrl.first()
            val savedUserId = settingsRepo.historyUserId.first()
            val embyUrl = settingsRepo.serverUrl.first()
            val inferredUrl = if (savedUrl.isBlank() && embyUrl.isNotBlank()) {
                inferHistoryUrl(embyUrl)
            } else savedUrl

            _uiState.update { it.copy(apiBaseUrl = inferredUrl, historyUserId = savedUserId) }
            historyRepo.setBaseUrl(inferredUrl)
            if (inferredUrl.isNotBlank()) refreshAll()
        }
    }

    // ── Config ──────────────────────────────────────────────

    fun updateApiBaseUrl(url: String) { _uiState.update { it.copy(apiBaseUrl = url) } }
    fun updateHistoryUserId(id: String) { _uiState.update { it.copy(historyUserId = id) } }
    fun updateSearchKeyword(kw: String) {
        _uiState.update { it.copy(searchKeyword = kw, currentPage = 1) }
    }

    fun saveConfig() {
        viewModelScope.launch {
            val url = _uiState.value.apiBaseUrl.trim()
            val normalized = if (url.isBlank()) "" else {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    _uiState.update { it.copy(error = "服务器地址必须以 http:// 或 https:// 开头") }
                    return@launch
                }
                url.trimEnd('/')
            }
            _uiState.update { it.copy(apiBaseUrl = normalized, error = null) }
            settingsRepo.setHistoryServiceUrl(normalized)
            settingsRepo.setHistoryUserId(_uiState.value.historyUserId)
            historyRepo.setBaseUrl(normalized)
        }
    }

    /** Save config and immediately refresh in a single coroutine to avoid race conditions. */
    fun saveConfigAndRefresh() {
        val url = _uiState.value.apiBaseUrl.trim()
        val normalized = if (url.isBlank()) "" else {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                _uiState.update { it.copy(error = "服务器地址必须以 http:// 或 https:// 开头") }
                return
            }
            url.trimEnd('/')
        }
        _uiState.update { it.copy(apiBaseUrl = normalized, error = null) }
        viewModelScope.launch {
            settingsRepo.setHistoryServiceUrl(normalized)
            settingsRepo.setHistoryUserId(_uiState.value.historyUserId)
            historyRepo.setBaseUrl(normalized)
            // Now refresh with the normalized URL
            refreshAll()
        }
    }

    fun clearUserFilter() {
        viewModelScope.launch {
            settingsRepo.setHistoryUserId("")
            _uiState.update { it.copy(historyUserId = "", currentPage = 1) }
            refreshCurrentSection()
        }
    }

    // ── Section switching with lazy load ─────────────────────

    fun switchSection(section: SectionType) {
        if (_uiState.value.currentSection == section) return
        _uiState.update {
            it.copy(
                currentSection = section,
                searchKeyword = "",
                selectedUser = "",
                selectedDevice = "",
                selectedType = "",
                sortField = "played_at",
                sortDirection = "desc",
                currentPage = 1
            )
        }
        // Lazy load: if the target section has no data, fetch it
        val hasData = when (section) {
            SectionType.HISTORY -> _uiState.value.historyData.isNotEmpty()
            SectionType.CONTINUE -> _uiState.value.continueData.isNotEmpty()
            SectionType.RECENT -> _uiState.value.recentData.isNotEmpty()
            SectionType.STATS -> _uiState.value.stats.totalPlays != 0 || _uiState.value.stats.finishedPlays != 0
        }
        if (!hasData) refreshCurrentSection()
    }

    // ── Data loading ─────────────────────────────────────────

    fun refreshAll() {
        val baseUrl = _uiState.value.apiBaseUrl.trim()
        if (baseUrl.isBlank() || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
            _uiState.update { it.copy(error = "请先配置有效的历史服务地址（以 http:// 或 https:// 开头）") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            historyRepo.setBaseUrl(baseUrl.trimEnd('/'))
            try {
                val stats = historyRepo.getStats().getOrElse { StatsResponse() }
                // FIX: pass historyUserId so the filter actually takes effect
                val userId = _uiState.value.historyUserId.trim().ifBlank { null }
                val history = historyRepo.getHistory(userId).getOrDefault(emptyList())
                val continueWatching = historyRepo.getContinueWatching().getOrDefault(emptyList())
                val recent = historyRepo.getRecent().getOrDefault(emptyList())

                _uiState.update {
                    it.copy(
                        stats = stats,
                        historyData = history,
                        continueData = continueWatching,
                        recentData = recent,
                        isLoading = false,
                        currentPage = 1
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun refreshCurrentSection() {
        val baseUrl = _uiState.value.apiBaseUrl.trim()
        if (baseUrl.isBlank() || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
            _uiState.update { it.copy(error = "请先配置有效的历史服务地址") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            historyRepo.setBaseUrl(baseUrl.trimEnd('/'))
            try {
                val section = _uiState.value.currentSection
                if (section == SectionType.STATS) {
                    val stats = historyRepo.getStats().getOrElse { StatsResponse() }
                    _uiState.update { it.copy(stats = stats, isLoading = false, currentPage = 1) }
                } else {
                    val userId = _uiState.value.historyUserId.trim().ifBlank { null }
                    when (section) {
                        SectionType.HISTORY -> {
                            val history = historyRepo.getHistory(userId).getOrDefault(emptyList())
                            _uiState.update { it.copy(historyData = history, isLoading = false, currentPage = 1) }
                        }
                        SectionType.CONTINUE -> {
                            val cont = historyRepo.getContinueWatching().getOrDefault(emptyList())
                            _uiState.update { it.copy(continueData = cont, isLoading = false, currentPage = 1) }
                        }
                        SectionType.RECENT -> {
                            val recent = historyRepo.getRecent().getOrDefault(emptyList())
                            _uiState.update { it.copy(recentData = recent, isLoading = false, currentPage = 1) }
                        }
                        SectionType.STATS -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ── Filters ──────────────────────────────────────────────

    fun setUserFilter(value: String) { _uiState.update { it.copy(selectedUser = value, currentPage = 1) } }
    fun setDeviceFilter(value: String) { _uiState.update { it.copy(selectedDevice = value, currentPage = 1) } }
    fun setTypeFilter(value: String) { _uiState.update { it.copy(selectedType = value, currentPage = 1) } }

    // ── Sorting ──────────────────────────────────────────────

    fun handleSort(field: String) {
        val state = _uiState.value
        if (state.currentSection == SectionType.CONTINUE) return
        if (state.sortField == field) {
            _uiState.update { it.copy(sortDirection = if (state.sortDirection == "asc") "desc" else "asc") }
        } else {
            val defaultDir = if (field == "title" || field == "type" || field == "user_name") "asc" else "desc"
            _uiState.update { it.copy(sortField = field, sortDirection = defaultDir) }
        }
    }

    fun sortIndicator(field: String): String {
        val state = _uiState.value
        if (state.sortField != field) return ""
        return if (state.sortDirection == "asc") " ↑" else " ↓"
    }

    // ── Pagination ───────────────────────────────────────────

    fun changePage(page: Int) {
        val state = _uiState.value
        if (page < 1 || page > totalPages || page == state.currentPage) return
        _uiState.update { it.copy(currentPage = page) }
    }

    // ── Detail ───────────────────────────────────────────────

    fun openDetail(record: PlayRecord) { _uiState.update { it.copy(selectedRecord = record) } }
    fun closeDetail() { _uiState.update { it.copy(selectedRecord = null) } }

    // ── Computed data ────────────────────────────────────────

    /** Raw data for the current section (STATS builds a virtual record). */
    val rawSectionData: List<PlayRecord> get() {
        val state = _uiState.value
        return when (state.currentSection) {
            SectionType.HISTORY -> state.historyData
            SectionType.CONTINUE -> state.continueData
            SectionType.RECENT -> state.recentData
            SectionType.STATS -> {
                if (state.stats.totalPlays == 0 && state.stats.finishedPlays == 0) emptyList()
                else listOf(
                    PlayRecord(
                        title = "数据概览",
                        type = "Stats",
                        userName = "系统统计",
                        playedAt = nowIso(),
                        progress = completionRate,
                        isFinished = 1,
                        client = "统计接口",
                        device = "Server",
                        duration = state.stats.totalDuration,
                        actualDuration = state.stats.actualTotalDuration,
                        eventType = "stats.snapshot"
                    )
                )
            }
        }
    }

    /** Whether the current section supports search + filter toolbar. */
    val supportsFilter: Boolean get() {
        val s = _uiState.value.currentSection
        return s == SectionType.HISTORY || s == SectionType.RECENT
    }

    /** Filtered + sorted list (before pagination). */
    val displayData: List<PlayRecord> get() {
        val state = _uiState.value
        var list = rawSectionData.toMutableList()

        if (supportsFilter) {
            val kw = state.searchKeyword.trim().lowercase()
            list = list.filter { item ->
                val matchKeyword = kw.isEmpty() ||
                    (item.title?.lowercase()?.contains(kw) == true) ||
                    (item.originalTitle?.lowercase()?.contains(kw) == true)
                val matchUser = state.selectedUser.isEmpty() || item.userName == state.selectedUser
                val matchDevice = state.currentSection != SectionType.HISTORY || state.selectedDevice.isEmpty() || item.device == state.selectedDevice
                val matchType = state.currentSection != SectionType.HISTORY || state.selectedType.isEmpty() || item.type == state.selectedType
                matchKeyword && matchUser && matchDevice && matchType
            }.toMutableList()
        }

        // Sort
        val field = state.sortField
        list.sortWith { a, b -> compareBySort(a, b, field, state.sortDirection) }

        return list
    }

    /** Paginated slice for the table view (continue section shows all). */
    val pagedData: List<PlayRecord> get() {
        val state = _uiState.value
        if (state.currentSection == SectionType.CONTINUE) return displayData
        val start = (state.currentPage - 1) * state.itemsPerPage
        if (start >= displayData.size) return emptyList()
        return displayData.subList(start, minOf(start + state.itemsPerPage, displayData.size))
    }

    val totalPages: Int get() {
        if (_uiState.value.currentSection == SectionType.CONTINUE) return 1
        return maxOf(1, (displayData.size + _uiState.value.itemsPerPage - 1) / _uiState.value.itemsPerPage)
    }

    val showPagination: Boolean get() = _uiState.value.currentSection != SectionType.CONTINUE && totalPages > 1

    val visiblePages: List<Int> get() {
        val current = _uiState.value.currentPage
        val total = totalPages
        val maxVisible = 5
        var start = maxOf(1, current - 2)
        var end = minOf(total, start + maxVisible - 1)
        if (end - start < maxVisible - 1) start = maxOf(1, end - maxVisible + 1)
        return (start..end).toList()
    }

    // Filter option lists derived from data
    val userOptions: List<String> get() = listOf("所有用户") + rawSectionData.mapNotNull { it.userName }.filter { it.isNotEmpty() }.distinct()

    val deviceOptions: List<String> get() = listOf("所有设备") + rawSectionData.mapNotNull { it.device }.filter { it.isNotEmpty() }.distinct()

    val typeOptions: List<String> get() = listOf("所有类型") + rawSectionData.mapNotNull { it.type }.filter { it.isNotEmpty() }.distinct()

    // Derived stats
    val completionRate: Int get() {
        val total = _uiState.value.stats.totalPlays
        if (total == 0) return 0
        return (_uiState.value.stats.finishedPlays * 100) / total
    }

    val averageWatchDuration: String get() {
        val total = _uiState.value.stats.totalPlays
        if (total == 0) return "00:00"
        return formatDuration(_uiState.value.stats.actualTotalDuration / total)
    }

    val filteredCountLabel: String get() = "共 ${displayData.size} 条"

    val currentUserFilterLabel: String get() = _uiState.value.selectedUser.ifBlank { "所有用户" }
    val currentDeviceFilterLabel: String get() = _uiState.value.selectedDevice.ifBlank { "所有设备" }
    val currentTypeFilterLabel: String get() {
        val t = _uiState.value.selectedType
        return if (t.isBlank()) "所有类型" else getTypeLabel(t)
    }

    // ── Sorting comparator ──────────────────────────────────

    private fun compareBySort(a: PlayRecord, b: PlayRecord, field: String, direction: String): Int {
        var av: Any? = null
        var bv: Any? = null
        when (field) {
            "title" -> { av = a.title; bv = b.title }
            "type" -> { av = a.type; bv = b.type }
            "user_name" -> { av = a.userName; bv = b.userName }
            "played_at" -> {
                av = parseDateMillis(a.playedAt ?: a.lastPlayedAt ?: a.eventTime)
                bv = parseDateMillis(b.playedAt ?: b.lastPlayedAt ?: b.eventTime)
            }
            else -> { av = a.title; bv = b.title }
        }
        val cmp = when {
            av is Long && bv is Long -> av.compareTo(bv)
            else -> {
                val as_ = (av?.toString() ?: "").lowercase()
                val bs_ = (bv?.toString() ?: "").lowercase()
                as_.compareTo(bs_)
            }
        }
        return if (direction == "asc") cmp else -cmp
    }

    private fun parseDateMillis(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        // Try ISO format
        return try {
            // Handle both "2026-07-27T12:00:00Z" and "2026-07-27 12:00:00"
            val cleaned = value.replace(' ', 'T')
            java.time.OffsetDateTime.parse(cleaned).toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                java.time.LocalDateTime.parse(value.replace(' ', 'T'))
                    .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            } catch (e2: Exception) {
                try { value.take(10).replace("-", "").toLong() } catch (e3: Exception) { 0L }
            }
        }
    }

    private fun nowIso(): String {
        return java.time.OffsetDateTime.now().toString()
    }

    // ── Format helpers ──────────────────────────────────────

    companion object {
        fun formatCount(value: Int): String = "%,d".format(value)

        fun formatPercent(value: Int?): String {
            val v = value ?: 0
            val clamped = v.coerceIn(0, 100)
            return "$clamped%"
        }

        fun formatDuration(seconds: Int): String {
            val total = maxOf(0, seconds)
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }

        fun formatDate(value: String?): String {
            if (value.isNullOrBlank()) return "-"
            val date = try {
                java.time.OffsetDateTime.parse(value.replace(' ', 'T'))
            } catch (e: Exception) {
                try {
                    val ldt = java.time.LocalDateTime.parse(value.replace(' ', 'T'))
                    ldt.atOffset(java.time.ZoneOffset.UTC)
                } catch (e2: Exception) { return value.take(10) }
            }
            return "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
        }

        fun formatDateTime(value: String?): String {
            if (value.isNullOrBlank()) return "-"
            val dt = try {
                java.time.OffsetDateTime.parse(value.replace(' ', 'T'))
            } catch (e: Exception) {
                try {
                    val ldt = java.time.LocalDateTime.parse(value.replace(' ', 'T'))
                    ldt.atOffset(java.time.ZoneOffset.UTC)
                } catch (e2: Exception) { return value }
            }
            return "%04d-%02d-%02d %02d:%02d".format(dt.year, dt.monthValue, dt.dayOfMonth, dt.hour, dt.minute)
        }

        fun getTypeLabel(type: String?): String = when (type) {
            "Movie", "movie" -> "电影"
            "Episode", "episode" -> "剧集"
            "Series", "series" -> "系列"
            "Season", "season" -> "季"
            "Video", "video" -> "视频"
            "Stats" -> "统计"
            else -> type ?: "未知"
        }

        fun inferHistoryUrl(embyUrl: String): String {
            if (embyUrl.isBlank()) return ""
            val trimmed = embyUrl.trim()
            // Parse URL to extract scheme and host, then replace/append port 3000
            // Handles: http://host:8096 → http://host:3000
            //          http://host → http://host:3000
            //          http://host:8096/emby → http://host:3000 (strips path)
            val regex = Regex("^(https?)://([^/:]+)(:\\d+)?")
            val match = regex.find(trimmed) ?: return "$trimmed:3000"
            val scheme = match.groupValues[1]
            val host = match.groupValues[2]
            return "$scheme://$host:3000"
        }
    }
}
