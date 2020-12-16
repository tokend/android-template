package io.tokend.template.features.account.data.model

import com.fasterxml.jackson.databind.JsonNode
import com.google.gson.annotations.SerializedName
import org.tokend.sdk.api.generated.resources.AccountResource
import java.io.Serializable
import java.util.*

class AccountRecord(
    @SerializedName("id")
    val id: String,
    @SerializedName("role_id")
    var roleId: Long,
    @SerializedName("kyc_recovery_status")
    var kycRecoveryStatus: KycRecoveryStatus,
    @SerializedName("kyc_blob_id")
    val kycBlob: String?
) : Serializable {
    constructor(source: AccountResource) : this(
        id = source.id,
        roleId = source.role.id.toLong(),
        kycRecoveryStatus = source
            .kycRecoveryStatus
            ?.name
            ?.toUpperCase(Locale.ENGLISH)
            ?.let(KycRecoveryStatus::valueOf)
            ?: KycRecoveryStatus.NONE,
        kycBlob = source
            .kycData
            ?.kycData
            ?.get("blob_id")
            ?.takeIf(JsonNode::isTextual)
            ?.asText()
    )

    enum class KycRecoveryStatus {
        NONE,
        INITIATED,
        PENDING,
        REJECTED,
        PERMANENTLY_REJECTED;
    }
}