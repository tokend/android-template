package io.tokend.template.extensions

/**
 * @return given [block] result or null if an exception was occurred
 */
inline fun <R : Any> tryOrNull(block: () -> R?) = try {
    block()
} catch (e: Exception) {
//    e.printStackTrace()
    null
}