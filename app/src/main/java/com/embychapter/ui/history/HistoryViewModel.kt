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
    val stats: Stats = Stats(),

    // Section
    val currentSection: SectionType = SectionType.HISTORY,
    val historyData: List<PlayRecord> = emptyList(),
    val continueData: List<PlayRecord> = emptyList(),
    val recentData: List<PlayRecord> = emptyList(),

    // Filters
    val searchKeyword: String = "",
    val selectedUser: String? = null,
    val selectedDevice: String? = null
)

enum class SectionType { HISTORY, CONTINUE, RECENT, STATS }
data class Stats(
    val totalPlays: Int = 0,
    val finishedPlays: Int = 0,
    val totalDuration: Int = 0,
    val actualTotalDuration: Int = 0
)

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
                embyUrl.replace(Regex(":\\d+$"), ":3000")
            } else savedUrl

            _uiState.update { it.copy(apiBaseUrl = inferredUrl, historyUserId = savedUserId) }
            historyRepo.setBaseUrl(inferredUrl)
            if (inferredUrl.isNotBlank()) refreshAll()
        }
    }

    fun updateApiBaseUrl(url: String) { _uiState.update { it.copy(apiBaseUrl = url) } }
    fun updateHistoryUserId(id: String) { _uiState.update { it.copy(historyUserId = id) } }
    fun updateSearchKeyword(kw: String) { _uiState.update { it.copy(searchKeyword = kw) } }

    fun saveConfig() {
        viewModelScope.launch {
            val url = _uiState.value.apiBaseUrl.trimEnd('/')
            _uiState.update { it.copy(apiBaseUrl = url) }
            settingsRepo.setHistoryServiceUrl(url)
            settingsRepo.setHistoryUserId(_uiState.value.historyUserId)
            historyRepo.setBaseUrl(url)
        }
    }

    fun switchSection(section: SectionType) {
        _uiState.update { it.copy(currentSection = section, searchKeyword = "", selectedUser = null, selectedDevice = null) }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            historyRepo.setBaseUrl(_uiState.value.apiBaseUrl.trimEnd('/'))
            try {
                val stats = historyRepo.getStats().getOrElse { StatsResponse() }
                val history = historyRepo.getHistory().getOrDefault(emptyList())
                val continueWatching = historyRepo.getContinueWatching().getOrDefault(emptyList())
                val recent = historyRepo.getRecent().getOrDefault(emptyList())

                _uiState.update {
                    it.copy(
                        stats = Stats(stats.totalPlays, stats.finishedPlays, stats.totalDuration, stats.actualTotalDuration),
                        historyData = history,
                        continueData = continueWatching,
                        recentData = recent,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    val filteredData: List<PlayRecord> get() {
        val state = _uiState.value
        val raw = when (state.currentSection) {
            SectionType.HISTORY -> state.historyData
            SectionType.CONTINUE -> state.continueData
            SectionType.RECENT -> state.recentData
            SectionType.STATS -> emptyList()
        }
        val kw = state.searchKeyword.trim().lowercase()
        return raw.filter {
            (kw.isEmpty() || (it.title?.lowercase()?.contains(kw) == true))
        }
    }

    companion object {
        fun formatDuration(seconds: Int): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }
    }
}
