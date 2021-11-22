package io.tokend.template.features.urlconfig.providers

import io.tokend.template.features.urlconfig.data.model.UrlConfig

class UrlConfigProviderImpl(
    defaultConfig: UrlConfig
): UrlConfigProvider {
    private var config: UrlConfig = defaultConfig

    override fun getConfig(): UrlConfig {
        return config
    }

    override fun setConfig(config: UrlConfig) {
        this.config = config
    }
}