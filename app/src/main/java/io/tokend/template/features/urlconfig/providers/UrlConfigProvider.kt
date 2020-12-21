package io.tokend.template.features.urlconfig.providers

import io.tokend.template.features.urlconfig.data.model.UrlConfig

interface UrlConfigProvider {
    fun hasConfig(): Boolean
    fun getConfig(): UrlConfig
    fun setConfig(config: UrlConfig)
}