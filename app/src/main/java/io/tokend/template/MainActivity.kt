package io.tokend.template

import android.os.Bundle
import io.tokend.template.base.activity.BaseActivity

class MainActivity : BaseActivity() {
    override val allowUnauthorized: Boolean = true

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
    }
}