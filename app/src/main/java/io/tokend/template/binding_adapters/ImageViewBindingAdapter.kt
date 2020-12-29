package io.tokend.template.binding_adapters

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import io.tokend.template.util.imagetransform.CircleTransform
import io.tokend.template.view.util.ImageViewUtil
import io.tokend.template.view.util.LogoFactory
import java.io.File

@BindingAdapter("avatarUrl", "userName", requireAll = true)
fun ImageView.loadCircleAvatarWithPlaceholder(url1: String?, userName: String?) {
    val sizePx: Int = (this.layoutParams as ViewGroup.LayoutParams).width
    val extras: Array<Any> = emptyArray()
    if (userName != null) {
        val placeholder = generateLogo(userName, this.context, sizePx, extras)

        ImageViewUtil.loadImage(this, url1, placeholder) {
            transform(CircleTransform())
        }
    }
}

@BindingAdapter("logoFile")
fun ImageView.loadCircleAvatarFromFile(file: File?) {
    val sizePx: Int = (this.layoutParams as ViewGroup.LayoutParams).width
    val extras: Array<Any> = emptyArray()
    if (file != null) {

        ImageViewUtil.loadImageFromFile(this, file) {
            transform(CircleTransform())
        }
    }
}

@BindingAdapter("photoFile")
fun ImageView.loadPhotoFromFile(file: File?) {
    val sizePx: Int = (this.layoutParams as ViewGroup.LayoutParams).width
    val extras: Array<Any> = emptyArray()
    if (file != null) {

        ImageViewUtil.loadImageFromFile(this, file) {

        }
    }
}

private fun generateLogo(
    content: String,
    context: Context,
    sizePx: Int,
    extras: Array<out Any>
): Drawable {
    return BitmapDrawable(
        context.resources,
        LogoFactory(context).getWithAutoBackground(content, sizePx, *extras)
    )
}