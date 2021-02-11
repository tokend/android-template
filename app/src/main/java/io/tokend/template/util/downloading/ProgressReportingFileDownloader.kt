package io.tokend.template.util.downloading

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposable
import io.tokend.template.logic.providers.ApiProvider
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.Util
import okio.BufferedSink
import okio.Okio
import okio.Source
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.factory.HttpClientFactory
import java.io.IOException

/**
 * Downloads files with progress reporting.
 *
 * It is assumed that you have an internet and storage permissions
 */
class ProgressReportingFileDownloader(
    private val contentResolver: ContentResolver,
    private val httpClientFactory: HttpClientFactory,
    private val withLogs: Boolean = false,
) {

    /**
     * @param fileKey TokenD storage key ([RemoteFile.key])
     * @param destinationContentUri URI given by [Intent.ACTION_CREATE_DOCUMENT] to write the file
     *
     * @return [Observable] that emits download progress percentage from 0 to 100
     */
    fun download(
        fileKey: String,
        apiProvider: ApiProvider,
        destinationContentUri: Uri,
    ): Observable<Int> =
        (apiProvider.getSignedApi() ?: throw IllegalStateException("No signed API instance found"))
            .documents
            .getUrl(fileKey)
            .toSingle()
            .flatMapObservable { sourceUrl ->
                download(sourceUrl, destinationContentUri)
            }

    /**
     * @param sourceUrl URL to download file from
     * @param destinationContentUri URI given by [Intent.ACTION_CREATE_DOCUMENT] to write the file
     *
     * @return [Observable] that emits download progress percentage from 0 to 100
     */
    fun download(
        sourceUrl: String,
        destinationContentUri: Uri,
    ): Observable<Int> = Observable.create<Int> { emitter ->
        if (destinationContentUri.scheme != "content") {
            emitter.tryOnError(IllegalArgumentException("Only content URIs are allowed"))
            return@create
        }

        var isDisposed = false
        var call: Call? = null
        emitter.setDisposable(object : Disposable {
            override fun isDisposed() = isDisposed

            override fun dispose() {
                call?.cancel()
                isDisposed = true
            }
        })

        var emittedProgress = -1
        val networkProgressListener = NetworkProgressListener { bytesRead, contentLength, done ->
            val progress = if (!done)
                (bytesRead * 100 / contentLength).toInt()
            else
                100
            if (progress != emittedProgress) {
                emittedProgress = progress
                emitter.onNext(progress)
            }
        }

        val httpClient = httpClientFactory
            .getBaseHttpClientBuilder(
                withLogs = withLogs
            )
            .addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(ProgressResponseBody(originalResponse.body(), networkProgressListener))
                    .build()
            }
            .build()

        val request = Request.Builder()
            .url(sourceUrl)
            .get()
            .build()

        if (isDisposed) {
            return@create
        }

        call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException) {
                emitter.tryOnError(e)
            }

            override fun onResponse(call: Call?, response: Response) {
                if (!isDisposed) {
                    dumpResponseToDestination(response, destinationContentUri, emitter)
                    emitter.onComplete()
                }
            }
        })
    }
        .doOnError { deleteDestinationQuietly(destinationContentUri) }
        .doOnDispose { deleteDestinationQuietly(destinationContentUri) }

    private fun dumpResponseToDestination(
        response: Response,
        destinationContentUri: Uri,
        errorEmitter: ObservableEmitter<*>
    ) {
        var destinationSink: BufferedSink? = null
        var bodySource: Source? = null

        try {
            val destinationOutputStream = contentResolver
                .openOutputStream(destinationContentUri, "w")
                ?: throw IllegalStateException("Could not get destination output stream from ContentResolver")

            destinationSink = Okio.buffer(Okio.sink(destinationOutputStream))
            bodySource = response.body().source()
            destinationSink.writeAll(bodySource)
        } catch (e: Throwable) {
            errorEmitter.tryOnError(e)
        } finally {
            Util.closeQuietly(destinationSink)
            Util.closeQuietly(bodySource)
        }
    }

    private fun deleteDestinationQuietly(destinationContentUri: Uri) = try {
        DocumentsContract.deleteDocument(contentResolver, destinationContentUri)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}