package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.ViewLayout
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.DarkThemeStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "librefiles_user_prefs")

data class AppUserPreferences(
    val isDarkMode: Boolean = true,
    val darkThemeStyle: DarkThemeStyle = DarkThemeStyle.SLATE,
    val accentChoice: AccentChoice = AccentChoice.CYAN,
    val isDynamicColor: Boolean = true,
    val isLowPowerMode: Boolean = false,
    val viewLayout: ViewLayout = ViewLayout.LIST,
    val showHiddenFiles: Boolean = false,
    val sortField: SortField = SortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING
)

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val DARK_THEME_STYLE = stringPreferencesKey("dark_theme_style")
        val ACCENT_CHOICE = stringPreferencesKey("accent_choice")
        val IS_DYNAMIC_COLOR = booleanPreferencesKey("is_dynamic_color")
        val IS_LOW_POWER_MODE = booleanPreferencesKey("is_low_power_mode")
        val VIEW_LAYOUT = stringPreferencesKey("view_layout")
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
    }

    val userPreferencesFlow: Flow<AppUserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isDark = preferences[PreferencesKeys.IS_DARK_MODE] ?: true
            val styleStr = preferences[PreferencesKeys.DARK_THEME_STYLE] ?: DarkThemeStyle.SLATE.name
            val style = try {
                DarkThemeStyle.valueOf(styleStr)
            } catch (e: Exception) {
                DarkThemeStyle.SLATE
            }

            val accentStr = preferences[PreferencesKeys.ACCENT_CHOICE] ?: AccentChoice.CYAN.name
            val accent = try {
                AccentChoice.valueOf(accentStr)
            } catch (e: Exception) {
                AccentChoice.CYAN
            }

            val layoutStr = preferences[PreferencesKeys.VIEW_LAYOUT] ?: ViewLayout.LIST.name
            val layout = try {
                ViewLayout.valueOf(layoutStr)
            } catch (e: Exception) {
                ViewLayout.LIST
            }

            val showHidden = preferences[PreferencesKeys.SHOW_HIDDEN_FILES] ?: false

            val sortFieldStr = preferences[PreferencesKeys.SORT_FIELD] ?: SortField.NAME.name
            val sortField = try {
                SortField.valueOf(sortFieldStr)
            } catch (e: Exception) {
                SortField.NAME
            }

            val sortDirStr = preferences[PreferencesKeys.SORT_DIRECTION] ?: SortDirection.ASCENDING.name
            val sortDir = try {
                SortDirection.valueOf(sortDirStr)
            } catch (e: Exception) {
                SortDirection.ASCENDING
            }

            val isDynamic = preferences[PreferencesKeys.IS_DYNAMIC_COLOR] ?: true
            val isLowPower = preferences[PreferencesKeys.IS_LOW_POWER_MODE] ?: false

            AppUserPreferences(
                isDarkMode = isDark,
                darkThemeStyle = style,
                accentChoice = accent,
                isDynamicColor = isDynamic,
                isLowPowerMode = isLowPower,
                viewLayout = layout,
                showHiddenFiles = showHidden,
                sortField = sortField,
                sortDirection = sortDir
            )
        }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun setDarkThemeStyle(style: DarkThemeStyle) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME_STYLE] = style.name
        }
    }

    suspend fun setAccentChoice(choice: AccentChoice) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_CHOICE] = choice.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setLowPowerMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_LOW_POWER_MODE] = enabled
        }
    }

    suspend fun setViewLayout(layout: ViewLayout) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIEW_LAYOUT] = layout.name
        }
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_HIDDEN_FILES] = show
        }
    }

    suspend fun setSortOption(field: SortField, direction: SortDirection) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_FIELD] = field.name
            preferences[PreferencesKeys.SORT_DIRECTION] = direction.name
        }
    }
}
