package io.tokend.template.base.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.LocalizedContextWrappingDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import io.reactivex.disposables.CompositeDisposable
import io.tokend.template.App
import io.tokend.template.BuildConfig
import io.tokend.template.R
import io.tokend.template.util.navigation.ActivityRequest
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.tfa.TfaVerifier

abstract class BaseActivity: AppCompatActivity(), TfaCallback {

    /**
     * If set to true the activity will be operational
     * even without account in [accountProvider] or with expired [session]
     */
    protected open val allowUnauthorized = false

    /**
     * Controls color scheme: default for guest, purple for non-guest (host).
     */
//    protected open val useHostColors: Boolean
//        get() = !session.isGuest //TODO: add session

    private var baseContextWrappingDelegate: AppCompatDelegate? = null

    /**
     * Disposable holder which will be disposed on activity destroy
     */
    protected val compositeDisposable: CompositeDisposable = CompositeDisposable()

    protected val activityRequestsBag: MutableCollection<ActivityRequest<*>> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.background)))

        super.onCreate(savedInstanceState)

//        (application as? App)?.stateComponent?.inject(this)

        if (BuildConfig.SECURE_CONTENT) {
            try {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

       /* if (accountProvider.getAccount() != null || allowUnauthorized) {
            // Change accent color for host.
            if (useHostColors) {
                theme.applyStyle(R.style.OverlayHostColor, true)
            }

            onCreateAllowed(savedInstanceState)
        } else {
            (application as App).signOut(this, soft = true)
            return
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val enterTransition = window?.enterTransition
            if (enterTransition != null) {
                enterTransition.excludeTarget(android.R.id.statusBarBackground, true)
                enterTransition.excludeTarget(android.R.id.navigationBarBackground, true)
            }
        }

        subscribeToLocaleChanges()
    }

    /**
     * You must implement your activity initialization here
     */
    abstract fun onCreateAllowed(savedInstanceState: Bundle?)

    override fun onStart() {
        super.onStart()
//        appTfaCallback.registerHandler(this)
    }

    override fun onDestroy() {
        super.onDestroy()
//        appTfaCallback.unregisterHandler(this)
        compositeDisposable.dispose()
    }

    override fun onResume() {
        super.onResume()

       /* if (session.isExpired) {
            session.reset()

            if (!allowUnauthorized) {
                (application as App).signOut(this, soft = true)
            }
        }*/
    }

    override fun onTfaRequired(exception: NeedTfaException,
                               verifierInterface: TfaVerifier.Interface) {
        runOnUiThread {
            /*val email = credentialsPersistence.getSavedLogin()
            TfaDialogFactory(this, errorHandlerFactory.getDefault(), appSharedPreferences,
                credentialsPersistence, toastManager)
                .getForException(exception, verifierInterface, email)
                ?.show()
                ?: verifierInterface.cancelVerification()*/
        }
    }

    // region Locale
    private fun subscribeToLocaleChanges() {
        /*localeManager
            .localeChanges
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { recreate() }
            .addTo(compositeDisposable)*/
    }

    override fun getDelegate() = baseContextWrappingDelegate ?: LocalizedContextWrappingDelegate(super.getDelegate(), App.localeManager.getLocale()).apply {
        baseContextWrappingDelegate = this
    }

   /* override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            *//*ViewPumpContextWrapper.wrap(
                App.appLocaleManager.getLocalizeContext(newBase)
            )*//*
        )
    }*/
    // endregion

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun finishWithError(cause: Throwable) {
//        errorHandlerFactory.getDefault().handle(cause)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        activityRequestsBag.forEach { request ->
            request.handleActivityResult(requestCode, resultCode, data)
        }

        activityRequestsBag.iterator().also { iterator ->
            while (iterator.hasNext()) {
                val request = iterator.next()
                if (request.isCompleted) {
                    iterator.remove()
                }
            }
        }
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        title = ""
    }
}