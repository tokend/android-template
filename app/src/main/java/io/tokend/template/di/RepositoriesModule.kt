package io.tokend.template.di

import android.content.Context
import android.content.SharedPreferences
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import io.tokend.template.db.AppDatabase
import io.tokend.template.features.urlconfig.providers.UrlConfigProvider
import io.tokend.template.logic.providers.*
import javax.inject.Named
import javax.inject.Singleton

@Module
class RepositoriesModule {
    @Provides
    @Singleton
    fun repositoriesProvider(
        apiProvider: ApiProvider,
        walletInfoProvider: WalletInfoProvider,
        urlConfigProvider: UrlConfigProvider,
        accountProvider: AccountProvider,
        mapper: ObjectMapper,
        context: Context,
        @Named("persistence")
        persistencePreferences: SharedPreferences,
        database: AppDatabase
    ): RepositoryProvider {
        return AppRepositoryProvider(
            apiProvider, walletInfoProvider, urlConfigProvider, accountProvider,
            mapper, context, persistencePreferences, database
        )
    }
}