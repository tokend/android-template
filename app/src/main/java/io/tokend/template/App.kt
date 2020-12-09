package io.tokend.template

import androidx.multidex.MultiDexApplication
import io.reactivex.subjects.BehaviorSubject
import io.tokend.template.util.locale.AppLocaleManager

class App: MultiDexApplication() {

    companion object {
        private const val GO_TO_BACKGROUND_TIMEOUT = 2000
        private const val IMAGE_CACHE_SIZE_MB = 8L
        private const val LOG_TAG = "TokenD App"
        private const val DATABASE_NAME = "app-db"

        /**
         * Emits value when app goes to the background or comes to the foreground.
         * [true] means that the app is currently in the background.
         */
        val backgroundStateSubject: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

        private lateinit var mLocaleManager: AppLocaleManager
        val localeManager: AppLocaleManager
            get() = mLocaleManager
    }
}