package com.embychapter.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "emby_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("emby_server_url")
        private val KEY_TOKEN = stringPreferencesKey("emby_token")
        private val KEY_USER_ID = stringPreferencesKey("emby_user_id")
        private val KEY_USER_NAME = stringPreferencesKey("emby_user_name")
        private val KEY_HISTORY_SERVICE_URL = stringPreferencesKey("history_service_url")
        private val KEY_HISTORY_USER_ID = stringPreferencesKey("history_user_id")
        private val KEY_VIDEO_CACHE = stringPreferencesKey("video_cache_json")
        private val KEY_LAST_SYNC_TIME = longPreferencesKey("video_last_sync_time")
        private val KEY_SORT_PREFERENCE = stringPreferencesKey("video_sort_pref")
    }

    // Server URL
    val serverUrl: Flow<String> = context.dataStore.data.map { it[KEY_SERVER_URL] ?: "" }
    suspend fun setServerUrl(url: String) = context.dataStore.edit { it[KEY_SERVER_URL] = url }

    // Token
    val token: Flow<String> = context.dataStore.data.map { it[KEY_TOKEN] ?: "" }
    suspend fun setToken(token: String) = context.dataStore.edit { it[KEY_TOKEN] = token }

    // User
    val userId: Flow<String> = context.dataStore.data.map { it[KEY_USER_ID] ?: "" }
    val userName: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "" }
    suspend fun setUser(id: String, name: String) = context.dataStore.edit {
        it[KEY_USER_ID] = id
        it[KEY_USER_NAME] = name
    }

    // History Service
    val historyServiceUrl: Flow<String> = context.dataStore.data.map { it[KEY_HISTORY_SERVICE_URL] ?: "" }
    suspend fun setHistoryServiceUrl(url: String) = context.dataStore.edit { it[KEY_HISTORY_SERVICE_URL] = url }

    val historyUserId: Flow<String> = context.dataStore.data.map { it[KEY_HISTORY_USER_ID] ?: "" }
    suspend fun setHistoryUserId(id: String) = context.dataStore.edit { it[KEY_HISTORY_USER_ID] = id }

    // Video cache (JSON-serialized EmbyItem list)
    val cachedVideos: Flow<String> = context.dataStore.data.map { it[KEY_VIDEO_CACHE] ?: "" }
    suspend fun setCachedVideos(json: String) = context.dataStore.edit { it[KEY_VIDEO_CACHE] = json }

    val lastSyncTime: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_SYNC_TIME] ?: 0L }
    suspend fun setLastSyncTime(time: Long) = context.dataStore.edit { it[KEY_LAST_SYNC_TIME] = time }

    /** Atomic save of cache JSON + timestamp in a single DataStore edit. */
    suspend fun saveVideoCache(json: String, timestamp: Long) = context.dataStore.edit {
        it[KEY_VIDEO_CACHE] = json
        it[KEY_LAST_SYNC_TIME] = timestamp
    }

    val sortPreference: Flow<String> = context.dataStore.data.map { it[KEY_SORT_PREFERENCE] ?: "RECENT" }
    suspend fun setSortPreference(sort: String) = context.dataStore.edit { it[KEY_SORT_PREFERENCE] = sort }

    // Clear all
    suspend fun clearAll() = context.dataStore.edit { it.clear() }
}
