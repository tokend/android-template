package io.tokend.template.logic.session

import android.content.SharedPreferences

class SessionInfoStorage(
    private val sharedPreferences: SharedPreferences
) {
    fun saveUserType(isGuest: Boolean?){
        sharedPreferences
            .edit()
            .putBoolean(USER_TYPE_KEY, isGuest ?: true)
            .apply()
    }
    fun saveFirebaseToken(token: String){
        sharedPreferences
            .edit()
            .putString(FIREBASE_TOKEN_KEY, token)
            .apply()
    }

    fun loadFirebaseToken(): String?{
        return sharedPreferences
            .getString(FIREBASE_TOKEN_KEY, null)
    }

    fun loadUserType(): Boolean{
        return sharedPreferences
            .getBoolean(USER_TYPE_KEY, true)
    }

    fun clear() {
        sharedPreferences
            .edit()
            .remove(USER_TYPE_KEY)
            .remove(FIREBASE_TOKEN_KEY)
            .apply()
    }

    private companion object {
        private const val USER_TYPE_KEY = "user_type"
        private const val FIREBASE_TOKEN_KEY = "firebase_token"
    }
}