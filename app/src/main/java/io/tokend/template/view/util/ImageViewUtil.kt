package io.tokend.template.view.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import io.tokend.template.util.imagetransform.CircleTransform
import java.io.File

object ImageViewUtil {
    fun loadImage(target: ImageView,
                  url: String?,
                  placeholder: Drawable?,
                  picassoCustomization: RequestCreator.() -> Unit = {}) {
        val picasso = Picasso.with(target.context)

        if (placeholder != null) {
            target.setImageDrawable(placeholder)
        }

        if (!url.isNullOrEmpty()) {
            picasso
                .load(url)
                .placeholder(placeholder)
                .apply(picassoCustomization)
                .into(target)
        } else {
            picasso.cancelRequest(target)
            if (placeholder != null) {
                target.setImageDrawable(placeholder)
            }
        }
    }

    fun loadImageFromFile(target: ImageView,
                          file: File?,
                          picassoCustomization: RequestCreator.() -> Unit = {}) {
        val picasso = Picasso.with(target.context)

        if (file != null) {
            picasso
                .load(file)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)

                .apply(picassoCustomization)
                .into(target)
        } else {
            picasso.cancelRequest(target)
        }
    }

    fun loadImageCircle(target: ImageView,
                        url: String?,
                        placeholder: Drawable?,
                        picassoCustomization: RequestCreator.() -> Unit = {}) {
        loadImage(target, url, placeholder) {
            apply(picassoCustomization)
            transform(CircleTransform())
            fit()
            centerCrop()
        }
    }

    fun cancelImageLoadingRequest(target: ImageView) {
        Picasso.with(target.context).cancelRequest(target)
    }
}