package io.tokend.template.features.accountidentity.data.model

import org.tokend.sdk.api.identity.model.IdentityResource

/**
 * Holds account identity data
 */
data class AccountIdentityRecord(
    val email: String,
    val accountId: String,
    var phoneNumber: String?,
    var telegramUsername: String?
) {
    constructor(source: IdentityResource) : this(
        email = source.email,
        accountId = source.address,
        phoneNumber = source.phoneNumber?.takeIf(String::isNotEmpty),
        telegramUsername = source.telegramUsername?.takeIf(String::isNotEmpty)
    )
}