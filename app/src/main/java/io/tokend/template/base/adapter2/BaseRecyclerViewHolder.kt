package io.tokend.template.base.adapter2

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Base abstract [RecyclerView.ViewHolder] for item of type [T]
 * with click listener
 */
abstract class BaseRecyclerViewHolder<T>(protected val view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(item: T)

    open fun bind(item: T, clickListener: SimpleItemClickListener<T>?) {
        bind(item)
        view.setOnClickListener { clickListener?.onViewClicked(view, item) }
    }
}