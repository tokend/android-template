package io.tokend.template.features.links

import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import io.reactivex.Completable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.R
import io.tokend.template.base.activity.BaseActivity
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.navigation.Navigator
import kotlinx.android.synthetic.main.layout_progress.*
import org.tokend.rx.extensions.toCompletable
import org.tokend.sdk.redirects.ClientRedirectPayload
import org.tokend.sdk.redirects.ClientRedirectType

class ProcessLinkActivity : BaseActivity() {
    override val allowUnauthorized = true

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_process_link)

        progress.show()

        progress.post {
            processIntentUrl()
        }
    }

    private fun processIntentUrl() {
        val intentData = intent?.data

        if (intentData == null) {
            finish()
            return
        }

        val url = intentData.toString()

        val payload = ClientRedirectPayload.fromUrl(url)
        if (payload != null && payload.type == ClientRedirectType.EMAIL_VERIFICATION) {
            performVerification(payload)
        } else {
            Log.i("LinkActivity", "Unknown URL $url, redirecting to the default activity")
            startActivity(packageManager.getLaunchIntentForPackage(packageName))
            overridePendingTransition(R.anim.stay_visible, R.anim.stay_visible)
            ActivityCompat.finishAfterTransition(this)
            return
        }
    }

    private fun performVerification(payload: ClientRedirectPayload) {
        val request =
            try {
                apiProvider.getApi()
                    .wallets
                    .verify(payload)
                    .toCompletable()
            } catch (e: Exception) {
                Completable.error(e)
            }

        request
            .compose(ObservableTransformers.defaultSchedulersCompletable())
            .subscribeBy(
                onComplete = {
                    toastManager.short(R.string.registration_has_been_verified)
                    Navigator.from(this).toSignIn(true)
                },
                onError = {
                    errorHandlerFactory.getDefault().handle(it)
                    finish()
                }
            )
            .addTo(compositeDisposable)
    }
}