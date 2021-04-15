package io.tokend.template.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment

inline fun <reified T : Fragment> Fragment.withArguments(bundle: Bundle) =
        apply { arguments = bundle } as T