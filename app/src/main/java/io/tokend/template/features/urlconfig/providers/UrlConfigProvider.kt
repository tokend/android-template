package io.tokend.template.features.urlconfig.providers

import io.tokend.template.features.urlconfig.data.model.UrlConfig

interface UrlConfigProvider {
    fun getConfig(): UrlConfig
    fun setConfig(config: UrlConfig)
}