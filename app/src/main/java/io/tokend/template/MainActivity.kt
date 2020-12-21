package io.tokend.template

import android.os.Bundle
import io.tokend.template.base.activity.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : BaseActivity() {

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)

        test_button.setOnClickListener {
            localeManager.setLocale(Locale("en"))
        }
    }
}