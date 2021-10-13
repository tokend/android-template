package io.tokend.template.view.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File

object ImageViewUtil {
    fun loadImage(
        target: ImageView,
        url: String?,
        placeholder: Drawable?,
        glideCustomization: RequestBuilder<*>.() -> Unit = {}
    ) {
        val glide = Glide.with(target.context)

        if (placeholder != null) {
            target.setImageDrawable(placeholder)
        }

        if (!url.isNullOrEmpty()) {
            glide
                .load(url)
                .placeholder(placeholder)
                .apply(glideCustomization)
                .into(target)
        } else {
            glide.clear(target)
            if (placeholder != null) {
                target.setImageDrawable(placeholder)
            }
        }
    }

    fun loadImageFromFile(
        target: ImageView,
        file: File?,
        glideCustomization: RequestBuilder<*>.() -> Unit = {}
    ) {
        val glide = Glide.with(target.context)

        if (file != null) {
            glide
                .load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .apply(glideCustomization)
                .into(target)
        } else {
            glide.clear(target)
        }
    }

    fun loadImageCircle(
        target: ImageView,
        url: String?,
        placeholder: Drawable?,
        glideCustomization: RequestBuilder<*>.() -> Unit = {}
    ) {
        loadImage(target, url, placeholder) {
            apply(glideCustomization)
            circleCrop()
        }
    }

    fun cancelImageLoadingRequest(target: ImageView) {
        Glide.with(target.context).clear(target)
    }
}