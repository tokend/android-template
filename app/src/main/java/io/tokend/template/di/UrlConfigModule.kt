package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.features.urlconfig.data.model.UrlConfig
import io.tokend.template.features.urlconfig.providers.UrlConfigProvider
import io.tokend.template.features.urlconfig.providers.UrlConfigProviderFactory
import javax.inject.Singleton

@Module
class UrlConfigModule(
    private val defaultConfig: UrlConfig
) {
    @Provides
    @Singleton
    fun urlConfigProvider(): UrlConfigProvider {
        return UrlConfigProviderFactory().createUrlConfigProvider(defaultConfig)
    }
}