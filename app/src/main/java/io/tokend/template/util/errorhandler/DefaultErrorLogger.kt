package io.tokend.template.util.errorhandler

import retrofit2.HttpException
import retrofit2.Response

class DefaultErrorLogger : ErrorLogger {
    override fun log(error: Throwable) {
        var e = error

        if (error is HttpException) {
            val rawResponse = error.response().raw()
            val request = rawResponse.request()
            e = HttpException(
                Response.error<Any>(
                    error.response().errorBody(),
                    rawResponse
                        .newBuilder()
                        .message(rawResponse.message() + " (${request.method()} ${request.url()})")
                        .build()
                )
            )
            e.stackTrace = emptyArray()
        }

        e.printStackTrace()
    }
}