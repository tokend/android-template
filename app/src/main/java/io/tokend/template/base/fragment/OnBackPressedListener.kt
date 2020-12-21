package io.tokend.template.base.fragment

interface OnBackPressedListener {
    /**
     * @returns: true if fragment needs to be closed, otherwise false
     */
    fun onBackPressed(): Boolean
}