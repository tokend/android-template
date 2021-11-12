package io.tokend.template.features.account.data.model

import com.fasterxml.jackson.databind.JsonNode
import com.google.gson.annotations.SerializedName
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import org.tokend.sdk.api.generated.resources.AccountResource
import java.io.Serializable
import java.util.*

class AccountRecord(
    @SerializedName("id")
    val id: String,
    @SerializedName("role")
    var role: ResolvedAccountRole,
    @SerializedName("kyc_recovery_status")
    var kycRecoveryStatus: KycRecoveryStatus,
    @SerializedName("kyc_blob_id")
    val kycBlob: String?,
) : Serializable {

    /**
     * @param keyValueEntries role-related key-value entries to resolve [AccountRole]
     *
     * @see NoSuchAccountRoleException
     */
    constructor(
        source: AccountResource,
        keyValueEntries: Collection<KeyValueEntryRecord>
    ) : this(
        id = source.id,
        role = ResolvedAccountRole(source.role.id.toLong(), keyValueEntries),
        kycRecoveryStatus = source
            .kycRecoveryStatus
            ?.name
            ?.toUpperCase(Locale.ENGLISH)
            ?.let(KycRecoveryStatus::valueOf)
            ?: KycRecoveryStatus.NONE,
        kycBlob = source.kycData?.kycData
            // Classics.
            ?.run { get("blob_id") ?: get("blobId") }
            ?.takeIf(JsonNode::isTextual)
            ?.asText()
            ?.takeIf(String::isNotEmpty),
    )

    enum class KycRecoveryStatus {
        NONE,
        INITIATED,
        PENDING,
        REJECTED,
        PERMANENTLY_REJECTED;
    }
}