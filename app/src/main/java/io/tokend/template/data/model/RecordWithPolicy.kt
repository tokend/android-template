package io.tokend.template.data.model

interface RecordWithPolicy {
    val policy: Int

    fun hasPolicy(value: Int): Boolean {
        return policy and value == value
    }
}