package io.tokend.template.base.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import io.tokend.template.util.navigation.ActivityRequest

abstract class BaseFragment : Fragment(), OnBackPressedListener {
    override fun onBackPressed() = true


    /**
     * Disposable holder which will be disposed on fragment destroy
     */
    protected lateinit var compositeDisposable: CompositeDisposable

    protected val activityRequestsBag: MutableCollection<ActivityRequest<*>> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        (activity?.application as? App)?.stateComponent?.inject(this)
        compositeDisposable = CompositeDisposable()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            onInitAllowed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
    }

    /**
     * You must implement your fragment initialization here
     */
    abstract fun onInitAllowed()

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
}