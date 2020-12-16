package io.tokend.template.data.storage.repository.pagination

/**
 * Interface for records, collections of
 * which are paged.
 */
interface PagingRecord {
    fun  getPagingId(): Long
}