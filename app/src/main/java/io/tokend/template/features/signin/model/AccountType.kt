package io.tokend.template.features.signin.model

import io.tokend.template.features.kyc.model.KycForm
import java.io.Serializable

sealed class AccountType(
    val roleId: Long
): Serializable {
    class Guest(roleId: Long) : AccountType(roleId) {
        companion object {
            const val ROLE_KEY = KycForm.General.ROLE_KEY //TODO implement guest role if needed
        }
    }

    class Host(roleId: Long) : AccountType(roleId) {
        companion object {
            const val ROLE_KEY = KycForm.General.ROLE_KEY //TODO implement host role if needed
        }
    }
}