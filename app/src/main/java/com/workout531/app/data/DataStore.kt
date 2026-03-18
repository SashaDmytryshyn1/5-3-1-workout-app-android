package com.workout531.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class DataStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("workout531", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveState(state: AppState) {
        prefs.edit().putString("app_state", gson.toJson(state)).apply()
    }

    fun loadState(): AppState {
        val json = prefs.getString("app_state", null) ?: return AppState()
        return try {
            gson.fromJson(json, AppState::class.java)
        } catch (e: Exception) {
            AppState()
        }
    }
}
