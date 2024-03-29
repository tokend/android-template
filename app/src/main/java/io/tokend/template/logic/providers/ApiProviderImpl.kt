package io.tokend.template.logic.providers

import io.tokend.template.BuildConfig
import io.tokend.template.features.urlconfig.providers.UrlConfigProvider
import okhttp3.CookieJar
import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.signing.AccountRequestSigner
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.utils.CookieJarProvider

class ApiProviderImpl(
    private val urlConfigProvider: UrlConfigProvider,
    private val accountProvider: AccountProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val tfaCallback: TfaCallback?,
    cookieJar: CookieJar?
) : ApiProvider {
    private val url: String
        get() = urlConfigProvider.getConfig().api

    private var cookieJarProvider = cookieJar?.let {
        object : CookieJarProvider {
            override fun getCookieJar(): CookieJar {
                return it
            }
        }
    }

    private val withLogs: Boolean
        get() = BuildConfig.WITH_LOGS

    private var apiByHash: Pair<Int, TokenDApi>? = null
    private var signedApiByHash: Pair<Int, TokenDApi>? = null

    override fun getApi(): TokenDApi = synchronized(this) {
        val hash = url.hashCode()

        val api = apiByHash
            ?.takeIf { (currentHash, _) ->
                currentHash == hash
            }
            ?.second
            ?: TokenDApi(
                url,
                null,
                tfaCallback,
                cookieJarProvider,
                withLogs = withLogs
            )

        apiByHash = Pair(hash, api)

        return api
    }

    override fun getKeyServer(): KeyServer {
        return KeyServer(getApi().wallets)
    }

    override fun getSignedApi(): TokenDApi = synchronized(this) {
        val account = accountProvider.getDefaultAccount()
        val originalAccountId = walletInfoProvider.getWalletInfo().accountId

        val hash = arrayOf(account.accountId, originalAccountId, url).contentHashCode()

        val signedApi =
            signedApiByHash
                ?.takeIf { (currentHash, _) ->
                    currentHash == hash
                }
                ?.second
                ?: TokenDApi(
                    url,
                    AccountRequestSigner(
                        account = account,
                        originalAccountId = originalAccountId
                    ),
                    tfaCallback,
                    cookieJarProvider,
                    withLogs = withLogs
                )

        signedApiByHash = Pair(hash, signedApi)

        return signedApi
    }

    override fun getSignedKeyServer(): KeyServer {
        return KeyServer(getSignedApi().wallets)
    }
}