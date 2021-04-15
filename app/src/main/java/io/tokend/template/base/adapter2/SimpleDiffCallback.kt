package io.tokend.template.base.adapter2

import androidx.recyclerview.widget.DiffUtil

abstract class SimpleDiffCallback<ItemType>(
    protected val oldItems: List<ItemType>,
    protected val newItems: List<ItemType>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(p0: Int, p1: Int): Boolean {
        return areItemsTheSame(oldItems[p0], newItems[p1], p0, p1)
    }

    abstract fun areItemsTheSame(
        old: ItemType,
        new: ItemType,
        oldPosition: Int,
        newPosition: Int
    ): Boolean

    override fun areContentsTheSame(p0: Int, p1: Int): Boolean {
        return areContentsTheSame(oldItems[p0], newItems[p1], p0, p1)
    }

    abstract fun areContentsTheSame(
        old: ItemType,
        new: ItemType,
        oldPosition: Int,
        newPosition: Int
    ): Boolean

    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size
}