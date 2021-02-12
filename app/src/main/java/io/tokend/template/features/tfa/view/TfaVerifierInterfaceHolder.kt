package io.tokend.template.features.tfa.view

import org.tokend.sdk.tfa.TfaVerifier

interface TfaVerifierInterfaceHolder {
    val tfaVerifierInterface: TfaVerifier.Interface?
}