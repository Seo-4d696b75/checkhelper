package com.seo4d696b75.android.ekisagasu.data.permission

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyHasLocationPermissionDenied = booleanPreferencesKey("has_location_permission_denied")
    private val keyHasNotificationPermissionDenied = booleanPreferencesKey("has_notification_permission_denied")

    suspend fun hasLocationPermissionDenied() = withContext(Dispatchers.IO) {
        context
            .dataStore
            .data
            .map { it[keyHasLocationPermissionDenied] ?: false }
            .first()
    }

    suspend fun hasNotificationPermissionDenied() = withContext(Dispatchers.IO) {
        context
            .dataStore
            .data
            .map { it[keyHasNotificationPermissionDenied] ?: false }
            .first()
    }

    suspend fun setLocationPermissionDenied() = withContext(Dispatchers.IO) {
        context
            .dataStore
            .edit { it[keyHasLocationPermissionDenied] = true }
    }

    suspend fun setNotificationPermissionDenied() = withContext(Dispatchers.IO) {
        context
            .dataStore
            .edit { it[keyHasNotificationPermissionDenied] = true }
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    // アプリのインストール毎に初期化される必要があるのでバックアップしない
    name = "permission_datastore",
)
