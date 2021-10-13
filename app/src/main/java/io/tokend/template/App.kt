package io.tokend.template

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.multidex.MultiDexApplication
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.BehaviorSubject
import io.tokend.template.db.AppDatabase
import io.tokend.template.di.*
import io.tokend.template.features.urlconfig.data.model.UrlConfig
import io.tokend.template.features.urlconfig.providers.UrlConfigProviderFactory
import io.tokend.template.logic.BackgroundLockManager
import io.tokend.template.logic.providers.AccountProviderFactory
import io.tokend.template.logic.providers.WalletInfoProviderFactory
import io.tokend.template.logic.session.Session
import io.tokend.template.logic.session.SessionInfoStorage
import io.tokend.template.util.locale.AppLocaleManager
import io.tokend.template.util.navigation.Navigator
import java.io.IOException
import java.net.SocketException
import java.util.*

class App : MultiDexApplication() {

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

    private var isInForeground = false
    private val goToBackgroundTimer = Timer()
    private var goToBackgroundTask: TimerTask? = null
    private var lastInForeground: Long = System.currentTimeMillis()
    private val logoutTime = BuildConfig.AUTO_LOGOUT

    private lateinit var sessionInfoStorage: SessionInfoStorage
    private lateinit var session: Session

    private lateinit var database: AppDatabase

    lateinit var stateComponent: AppStateComponent

    private val areGooglePlayServicesAvailable: Boolean
        get() {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
            return resultCode == ConnectionResult.SUCCESS
        }

    override fun onCreate() {
        super.onCreate()
        try {
            if (areGooglePlayServicesAvailable) {
                ProviderInstaller.installIfNeeded(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) {
                setIsInForeground(true)
            }

            override fun onActivityPaused(a: Activity) {
                setIsInForeground(false)
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) {}

            override fun onActivityStarted(a: Activity) {}

            override fun onActivityStopped(a: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(a: Activity) {}
        })

        initLocale()
        initState()
        initGlide()
        initRxErrorHandler()
    }

    private fun initLocale() {
        mLocaleManager = AppLocaleManager(this, getAppPreferences())
        localeManager.initLocale()
    }

    private fun initGlide() {
        Glide.init(this, GlideBuilder().apply {
            setDiskCache(
                InternalCacheDiskCacheFactory(
                    this@App,
                    cacheDir.absolutePath,
                    IMAGE_CACHE_SIZE_MB * 1024 * 1024
                )
            )
            setDefaultTransitionOptions(
                Drawable::class.java,
                DrawableTransitionOptions.withCrossFade(
                    DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                )
            )
        })
    }

    private fun initRxErrorHandler() {
        RxJavaPlugins.setErrorHandler {
            var e = it
            if (e is UndeliverableException) {
                e = e.cause
            }
            if ((e is IOException) || (e is SocketException)) {
                // fine, irrelevant network problem or API that throws on cancellation
                return@setErrorHandler
            }
            if (e is InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return@setErrorHandler
            }
            if ((e is NullPointerException) || (e is IllegalArgumentException)) {
                // that's likely a bug in the application
                Thread.currentThread().uncaughtExceptionHandler
                    ?.uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }
            if (e is IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().uncaughtExceptionHandler
                    ?.uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }
            Log.w("RxErrorHandler", "Undeliverable exception received, not sure what to do", e)
        }
    }

    // region State
    // region Preferences
    private fun getPersistencePreferences(): SharedPreferences {
        return getSharedPreferences("persistence", Context.MODE_PRIVATE)
    }

    private fun getAppPreferences(): SharedPreferences {
        return getSharedPreferences("app", Context.MODE_PRIVATE)
    }
    // endregion

    private fun getDatabase(): AppDatabase {
        return Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
    }

    private fun initState() {
        sessionInfoStorage = SessionInfoStorage(getAppPreferences())
        session = Session(
            WalletInfoProviderFactory().createWalletInfoProvider(),
            AccountProviderFactory().createAccountProvider(),
            sessionInfoStorage
        )

        database = getDatabase()

        val defaultUrlConfig = UrlConfig(
            BuildConfig.API_URL, BuildConfig.STORAGE_URL,
            BuildConfig.CLIENT_URL
        )
        val urlConfigProvider = UrlConfigProviderFactory().createUrlConfigProvider(defaultUrlConfig)

        stateComponent = DaggerAppStateComponent.builder()
            .appModule(AppModule(this))
            .urlConfigProviderModule(UrlConfigProviderModule(urlConfigProvider))
            .apiProviderModule(ApiProviderModule(null))
            .persistenceModule(
                PersistenceModule(
                    appPreferences = getAppPreferences(),
                    persistencePreferences = getPersistencePreferences(),
                )
            )
            .sessionModule(SessionModule(session))
            .localeManagerModule(LocaleManagerModule(localeManager))
            .appDatabaseModule(AppDatabaseModule(database))
            .build()
    }

    private fun clearUserData() {
        sessionInfoStorage.clear()
        getPersistencePreferences().edit().clear().apply()
        Thread { database.clearAllTables() }.start()
    }

    /**
     * @param soft if set to true user data will not be cleared.
     */
    @SuppressLint("ApplySharedPref")
    fun signOut(activity: Activity?, soft: Boolean = false) {
        session.reset()

        if (!soft) {
            clearUserData()
            initState()
//             Uncomment this if using Firebase messaging
//            Thread { FirebaseInstanceId.getInstance().deleteInstanceId() }.start()
        }

        Navigator.from(this).toSignIn() //TODO: implement toSignIn method in Navigator class

        activity?.let {
            it.setResult(Activity.RESULT_CANCELED, null)
            ActivityCompat.finishAffinity(it)
        }
    }
    // endregion

    // region Background/Foreground state.
    fun setIsInForeground(isInForeground: Boolean) {
        if (isInForeground) {
            cancelBackgroundCallback()
        }

        if (this.isInForeground == isInForeground) {
            return
        }

        if (isInForeground) {
            this.isInForeground = true
            onAppComesToForeground()
        } else {
            scheduleBackgroundCallback()
        }
    }

    private fun scheduleBackgroundCallback() {
        cancelBackgroundCallback()
        goToBackgroundTask = getGoToBackgroundTask()
        goToBackgroundTimer.schedule(goToBackgroundTask, GO_TO_BACKGROUND_TIMEOUT.toLong())
    }

    private fun getGoToBackgroundTask(): TimerTask {
        return object : TimerTask() {
            override fun run() {
                this@App.isInForeground = false
                onAppGoesToBackground()
            }
        }
    }

    private fun cancelBackgroundCallback() {
        if (goToBackgroundTask != null) {
            goToBackgroundTask?.cancel()
        }

        goToBackgroundTimer.purge()
    }

    private fun onAppGoesToBackground() {
        Log.d(LOG_TAG, "onAppGoesToBackground()")
        lastInForeground = System.currentTimeMillis()
        backgroundStateSubject.onNext(true)
    }

    private fun onAppComesToForeground() {
        Log.d(LOG_TAG, "onAppComesToForeground()")
        backgroundStateSubject.onNext(false)

        expireSessionIfNeeded()

        lastInForeground = 0
    }

    private fun expireSessionIfNeeded() {
        val now = System.currentTimeMillis()
        val backgroundLockManager = BackgroundLockManager(getAppPreferences())
        session.isExpired = backgroundLockManager.isBackgroundLockEnabled &&
                logoutTime != 0L && now - lastInForeground > logoutTime
    }
    // endregion
}