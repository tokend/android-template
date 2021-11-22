package io.tokend.template.features.urlconfig.providers

import io.tokend.template.features.urlconfig.data.model.UrlConfig

class UrlConfigProviderFactory {
    fun createUrlConfigProvider(defaultConfig: UrlConfig): UrlConfigProvider {
        return UrlConfigProviderImpl(defaultConfig)
    }
}