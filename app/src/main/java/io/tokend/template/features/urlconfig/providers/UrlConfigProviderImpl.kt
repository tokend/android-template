package io.tokend.template.features.urlconfig.providers

import io.tokend.template.features.urlconfig.data.model.UrlConfig

class UrlConfigProviderImpl : UrlConfigProvider {
    private var config: UrlConfig? = null

    override fun hasConfig(): Boolean {
        return config != null
    }

    override fun getConfig(): UrlConfig {
        return config!!
    }

    override fun setConfig(config: UrlConfig) {
        this.config = config
    }
}