package io.tokend.template.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.logic.credentials.persistence.CredentialsPersistenceOnPreferences
import io.tokend.template.logic.credentials.persistence.WalletInfoPersistence
import io.tokend.template.logic.credentials.persistence.WalletInfoPersistenceOnPreferences
import javax.inject.Named
import javax.inject.Singleton

@Module
class PersistenceModule(
    private val appPreferences: SharedPreferences,
    private val persistencePreferences: SharedPreferences,
) {
    @Provides
    @Singleton
    fun credentialsPersistence(): CredentialsPersistence {
        return CredentialsPersistenceOnPreferences(persistencePreferences)
    }

    @Provides
    @Singleton
    fun walletInfoPersistence(): WalletInfoPersistence {
        return WalletInfoPersistenceOnPreferences(persistencePreferences)
    }

    @Provides
    @Named("persistence")
    @Singleton
    fun persistencePreferences(): SharedPreferences {
        return persistencePreferences
    }

    @Provides
    @Named("app")
    @Singleton
    fun appPreferences(): SharedPreferences {
        return appPreferences
    }
}