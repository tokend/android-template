package io.tokend.template.features.signin.model

import io.tokend.template.features.kyc.model.KycForm
import io.tokend.template.features.signin.logic.FindOutAccountTypeUseCase
import java.io.Serializable

/**
 * @see FindOutAccountTypeUseCase
 */
sealed class AccountType(
    val roleId: Long
) : Serializable {
    // Implement your types if required.
    // Do not forget to include role keys to FindOutAccountTypeUseCase.getActualRoles()

    class User(roleId: Long) : AccountType(roleId) {
        companion object {
            const val ROLE_KEY = KycForm.General.ROLE_KEY
        }
    }
}