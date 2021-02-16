package io.tokend.template.base.activity

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.tokend.template.App
import io.tokend.template.BuildConfig
import io.tokend.template.R
import io.tokend.template.features.signin.logic.PostSignInManagerFactory
import io.tokend.template.features.tfa.logic.AppTfaCallback
import io.tokend.template.features.tfa.view.TfaDialogFactory
import io.tokend.template.features.urlconfig.providers.UrlConfigProvider
import io.tokend.template.logic.BackgroundLockManager
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.logic.credentials.persistence.WalletInfoPersistence
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.logic.session.Session
import io.tokend.template.util.ConnectionStateUtil
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.errorhandler.ErrorHandlerFactory
import io.tokend.template.util.locale.AppLocaleManager
import io.tokend.template.util.navigation.ActivityRequest
import io.tokend.template.view.ProgressDialog
import io.tokend.template.view.ToastManager
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.tfa.TfaVerifier
import javax.inject.Inject
import javax.inject.Named

abstract class BaseActivity : AppCompatActivity(), TfaCallback {

    @Inject
    lateinit var appTfaCallback: AppTfaCallback

    @Inject
    lateinit var accountProvider: AccountProvider

    @Inject
    lateinit var apiProvider: ApiProvider

    @Inject
    lateinit var walletInfoProvider: WalletInfoProvider

    @Inject
    lateinit var repositoryProvider: RepositoryProvider

    @Inject
    lateinit var credentialsPersistence: CredentialsPersistence

    @Inject
    lateinit var walletInfoPersistence: WalletInfoPersistence

    @Inject
    lateinit var urlConfigProvider: UrlConfigProvider

    @Inject
    lateinit var errorHandlerFactory: ErrorHandlerFactory

    @Inject
    lateinit var toastManager: ToastManager

    @Inject
    lateinit var session: Session

    @Inject
    lateinit var localeManager: AppLocaleManager

    @Inject
    lateinit var backgroundLockManager: BackgroundLockManager

    @Inject
    lateinit var postSignInManagerFactory: PostSignInManagerFactory

    @Inject
    lateinit var connectionStateUtil: ConnectionStateUtil

    @Inject
    @Named("app")
    lateinit var appSharedPreferences: SharedPreferences

    /**
     * If set to true the activity will be operational
     * even without account in [accountProvider] or with expired [session]
     */
    protected open val allowUnauthorized = false

    private var baseContextWrappingDelegate: AppCompatDelegate? = null

    /**
     * Disposable holder which will be disposed on activity destroy
     */
    protected val compositeDisposable: CompositeDisposable = CompositeDisposable()

    protected val activityRequestsBag: MutableCollection<ActivityRequest<*>> = mutableSetOf()

    private val progressDialog = ProgressDialog()

    var isLoading = MutableLiveData<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    this,
                    R.color.background
                )
            )
        )

        super.onCreate(savedInstanceState)

        (application as? App)?.stateComponent?.inject(this)

        if (BuildConfig.SECURE_CONTENT) {
            try {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        isLoading.observe(this, Observer {
            if (it) {
                progressDialog.show(this, "Loading...")
            } else {
                progressDialog.dialog?.dismiss()
            }
        })

        if (accountProvider.getAccount() != null || allowUnauthorized) {
            onCreateAllowed(savedInstanceState)
        } else {
            Log.i(
                "BaseActivity",
                "Missing account and allowUnauthorized=false. Making soft sign out"
            )
            (application as App).signOut(this, soft = true)
            return
        }

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
        appTfaCallback.registerHandler(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        appTfaCallback.unregisterHandler(this)
        compositeDisposable.dispose()
    }

    override fun onResume() {
        super.onResume()

        if (session.isExpired) {
            session.reset()

            if (!allowUnauthorized) {
                (application as App).signOut(this, soft = true)
            }
        }
    }

    override fun onTfaRequired(
        exception: NeedTfaException,
        verifierInterface: TfaVerifier.Interface
    ) {
        runOnUiThread {
            TfaDialogFactory(this, errorHandlerFactory.getDefault(), toastManager)
                .getForException(exception, verifierInterface, session.login)
                ?.show()
                ?: verifierInterface.cancelVerification()
        }
    }

    // region Locale
    private fun subscribeToLocaleChanges() {
        localeManager
            .localeChanges
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { recreate() }
            .addTo(compositeDisposable)
    }

    override fun getDelegate() = baseContextWrappingDelegate
        ?: App.localeManager.getLocalizeContextWrapperDelegate(super.getDelegate()).apply {
            baseContextWrappingDelegate = this
        }
    // endregion

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun finishWithError(cause: Throwable) {
        errorHandlerFactory.getDefault().handle(cause)
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