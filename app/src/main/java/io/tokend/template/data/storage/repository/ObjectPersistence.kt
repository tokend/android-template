package io.tokend.template.data.storage.repository

interface ObjectPersistence<T: Any> {
    fun loadItem(): T?
    fun saveItem(item: T)
    fun hasItem(): Boolean
    fun clear()
}