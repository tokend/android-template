package io.tokend.template.features.kyc.model

import com.google.gson.annotations.SerializedName
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.sdk.api.documents.model.DocumentType
import org.tokend.sdk.factory.GsonFactory
import java.util.*

/**
 * KYC form data with documents
 */
abstract class KycForm(
    @SerializedName("documents")
    var documents: MutableMap<String, RemoteFile>? = mutableMapOf()
) {
    // In order to avoid serialization it is declared as a method.
    abstract fun getRoleKey(): String

    open fun getDocument(type: DocumentType): RemoteFile? {
        return documents?.get(type.name.toLowerCase(Locale.ENGLISH))
    }

    open fun setDocument(type: DocumentType, file: RemoteFile?) {
        if (file == null) {
            documents?.remove(type.name.toLowerCase(Locale.ENGLISH))
            return
        }

        documents?.put(type.name.toLowerCase(Locale.ENGLISH), file)
    }

    /**
     * Implement your KYC forms here according to the example below
     */

    class General(
        @SerializedName("first_name")
        val firstName: String,
        @SerializedName("last_name")
        val lastName: String,
        documents: MutableMap<String, RemoteFile>? = null
    ) : KycForm(documents) {
        val avatar: RemoteFile?
            get() = documents?.get("kyc_avatar")

        val fullName: String
            get() = "$firstName $lastName"

        override fun getRoleKey(): String =
            ROLE_KEY

        companion object {
            const val ROLE_KEY = "$ROLE_KEY_PREFIX:general"
        }
    }

    /**
     * Empty form to use in case when the original form
     * can't be processed
     */
    object Empty : KycForm() {
        override fun getRoleKey(): String {
            throw IllegalArgumentException("You can't use empty form to change role")
        }
    }

    companion object {
        private const val ROLE_KEY_PREFIX = "account_role"

        /**
         * Finds out KYC form type by the name of corresponding [roleId]
         *
         * @param blob KYC form blob
         */
        fun fromBlob(
            blob: Blob,
            roleId: Long,
            keyValueEntries: Collection<KeyValueEntryRecord>
        ): KycForm {
            return fromJson(blob.valueString, roleId, keyValueEntries)
        }

        /**
         * Finds out KYC form type by the name of corresponding [roleId]
         *
         * @param json KYC form JSON
         */
        fun fromJson(
            json: String,
            roleId: Long,
            keyValueEntries: Collection<KeyValueEntryRecord>
        ): KycForm {
            val gson = GsonFactory().getBaseGson()
            val roleKey = keyValueEntries
                .find {
                    it.key.startsWith(ROLE_KEY_PREFIX)
                            && it is KeyValueEntryRecord.Number
                            && it.value == roleId
                }
                ?.key
                ?: throw IllegalArgumentException("Role $roleId has no corresponding key-value entry")

            return when (roleKey) {
                General.ROLE_KEY ->
                    gson.fromJson(json, General::class.java)
                else ->
                    // Replace with your custom "wrong role" exception
                    throw IllegalArgumentException("Unknown KYC form type")
            }
        }
    }
}