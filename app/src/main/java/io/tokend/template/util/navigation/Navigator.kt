package io.tokend.template.util.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import io.tokend.template.R
import io.tokend.template.util.IntentLock

/**
 * Performs transitions between screens.
 * 'open-' will open related screen as a child.<p>
 * 'to-' will open related screen and finish current.
 */
class Navigator private constructor() {
    private var activity: Activity? = null
    private var fragment: Fragment? = null
    private var context: Context? = null

    companion object {
        fun from(activity: Activity): Navigator {
            val navigator = Navigator()
            navigator.activity = activity
            navigator.context = activity
            return navigator
        }

        fun from(fragment: Fragment): Navigator {
            val navigator = Navigator()
            navigator.fragment = fragment
            navigator.context = fragment.requireContext()
            return navigator
        }

        fun from(context: Context): Navigator {
            val navigator = Navigator()
            navigator.context = context
            return navigator
        }
    }

    private fun performIntent(
        intent: Intent?,
        requestCode: Int? = null,
        activityOptionsBundle: Bundle? = null
    ) {
        if (intent != null) {
            if (!IntentLock.checkIntent(intent, context)) return
            activity?.let {
                if (requestCode != null) {
                    ActivityCompat.startActivityForResult(
                        it,
                        intent,
                        requestCode,
                        activityOptionsBundle
                    )
                } else {
                    ActivityCompat.startActivity(it, intent, activityOptionsBundle)
                }
                return
            }

            fragment?.let {
                if (requestCode != null) {
                    it.startActivityForResult(intent, requestCode, activityOptionsBundle)
                } else {
                    it.startActivity(intent, activityOptionsBundle)
                }
                return
            }

            context?.let {
                ActivityCompat.startActivity(
                    it,
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    activityOptionsBundle
                )
            }
        }
    }

    private fun createAndPerformIntent(
        activityClass: Class<*>,
        extras: Bundle? = null,
        requestCode: Int? = null,
        transitionBundle: Bundle? = null,
        intentModifier: (Intent.() -> Intent)? = null
    ) {
        var intent = context?.let { Intent(it, activityClass) }
            ?: return

        if (extras != null) {
            intent.putExtras(extras)
        }

        if (intentModifier != null) {
            intent = intentModifier.invoke(intent)
        }

        performIntent(intent, requestCode, transitionBundle)
    }

    private fun <R : Any> createAndPerformRequest(
        request: ActivityRequest<R>,
        activityClass: Class<*>,
        extras: Bundle? = null,
        transitionBundle: Bundle? = null
    ): ActivityRequest<R> {
        createAndPerformIntent(activityClass, extras, request.code, transitionBundle)
        return request
    }

    private fun createAndPerformSimpleRequest(
        activityClass: Class<*>,
        extras: Bundle? = null,
        transitionBundle: Bundle? = null
    ) = createAndPerformRequest(
        ActivityRequest.withoutResultData(),
        activityClass, extras, transitionBundle
    )

    private fun fadeOut(
        activity: Activity,
        finishAffinity: Boolean
    ) {
        ActivityCompat.finishAfterTransition(activity)
        activity.overridePendingTransition(0, R.anim.activity_fade_out)
        if (finishAffinity) {
            activity.setResult(Activity.RESULT_CANCELED, null)
            ActivityCompat.finishAffinity(activity)
        } else {
            activity.finish()
        }
    }

    private fun createTransitionBundle(
        activity: Activity,
        vararg pairs: Pair<View?, String>
    ): Bundle {
        val sharedViews = arrayListOf<androidx.core.util.Pair<View, String>>()

        pairs.forEach {
            val view = it.first
            if (view != null) {
                sharedViews.add(androidx.core.util.Pair(view, it.second))
            }
        }

        return if (sharedViews.isEmpty()) {
            Bundle.EMPTY
        } else {
            ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                *sharedViews.toTypedArray()
            ).toBundle() ?: Bundle.EMPTY
        }
    }

    fun toSignIn(finishAffinity: Boolean = false) {
        /*Intent(context, SignInPhoneNumberInputActivity::class.java) //TODO implement SignInActivity
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .also { performIntent(it) }
        activity?.let {
            if (finishAffinity) {
                it.setResult(Activity.RESULT_CANCELED, null)
                ActivityCompat.finishAffinity(it)
            } else {
                it.finish()
            }
        }*/
    }

    fun openFileSaveDestinationPicker(
        fileName: String,
        mimeType: String
    ): ActivityRequest<Uri> {
        val request = ActivityRequest { result ->
            result?.data
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_TITLE, fileName)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        performIntent(intent, request.code)

        return request
    }
}
