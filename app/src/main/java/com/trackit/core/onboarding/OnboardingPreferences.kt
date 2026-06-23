package com.trackit.core.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "trackit_onboarding"
)

class OnboardingPreferences(private val context: Context) {

    suspend fun hasCompletedAppOnboarding(): Boolean {
        return context.onboardingDataStore.data
            .map { prefs -> prefs[KEY_APP_ONBOARDING_DONE] ?: false }
            .first()
    }

    suspend fun setAppOnboardingCompleted() {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_APP_ONBOARDING_DONE] = true
        }
    }

    suspend fun hasCompletedRoleTour(userId: String): Boolean {
        return context.onboardingDataStore.data
            .map { prefs -> prefs[KEY_ROLE_TOUR_USERS]?.contains(userId) == true }
            .first()
    }

    suspend fun setRoleTourCompleted(userId: String) {
        context.onboardingDataStore.edit { prefs ->
            val current = prefs[KEY_ROLE_TOUR_USERS] ?: emptySet()
            prefs[KEY_ROLE_TOUR_USERS] = current + userId
        }
    }

    suspend fun resetRoleTour(userId: String) {
        context.onboardingDataStore.edit { prefs ->
            val current = prefs[KEY_ROLE_TOUR_USERS] ?: emptySet()
            prefs[KEY_ROLE_TOUR_USERS] = current - userId
        }
    }

    companion object {
        private val KEY_APP_ONBOARDING_DONE = booleanPreferencesKey("app_onboarding_done")
        private val KEY_ROLE_TOUR_USERS = stringSetPreferencesKey("role_tour_completed_user_ids")
    }
}
