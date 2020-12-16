package io.tokend.template.features.logic.providers

import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.keyserver.KeyServer

interface ApiProvider {
    fun getApi(): TokenDApi
    fun getSignedApi(): TokenDApi?
    fun getKeyServer(): KeyServer
    fun getSignedKeyServer(): KeyServer?
}