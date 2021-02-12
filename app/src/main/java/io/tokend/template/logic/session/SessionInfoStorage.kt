package io.tokend.template.logic.session

import android.content.SharedPreferences

class SessionInfoStorage(
    private val sharedPreferences: SharedPreferences
) {
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

    fun clear() {
        sharedPreferences
            .edit()
            .remove(FIREBASE_TOKEN_KEY)
            .apply()
    }

    private companion object {
        private const val FIREBASE_TOKEN_KEY = "firebase_token"
    }
}