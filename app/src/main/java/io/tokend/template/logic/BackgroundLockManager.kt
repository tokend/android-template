package io.tokend.template.logic

import android.content.SharedPreferences
import android.os.Build
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence

/**
 * Manages background app lock preference.
 */
class BackgroundLockManager(private val appSharedPreferences: SharedPreferences) {

    /**
     * Depends on whether secure storage for password is available.
     *
     * @see CredentialsPersistence
     */
    val canBackgroundLockBeDisabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    /**
     * If enabled then lock screen must ask for auth.
     */
    var isBackgroundLockEnabled
        get() = appSharedPreferences.getBoolean(PREFERENCE_KEY, ENABLED_BY_DEFAULT)
        set(value) {
            appSharedPreferences.edit().putBoolean(PREFERENCE_KEY, value).apply()
        }

    companion object {
        private const val ENABLED_BY_DEFAULT = true
        private const val PREFERENCE_KEY = "background_lock"
    }
}