package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.features.urlconfig.providers.UrlConfigProvider
import javax.inject.Singleton

@Module
class UrlConfigProviderModule(
    private val urlConfigProvider: UrlConfigProvider
) {
    @Provides
    @Singleton
    fun urlConfigProvider(): UrlConfigProvider {
        return urlConfigProvider
    }
}