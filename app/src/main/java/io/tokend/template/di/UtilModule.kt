package io.tokend.template.di

import android.content.Context
import android.content.SharedPreferences
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import io.tokend.template.logic.BackgroundLockManager
import io.tokend.template.util.ConnectionStateUtil
import io.tokend.template.util.cipher.Aes256GcmDataCipher
import io.tokend.template.util.cipher.DataCipher
import io.tokend.template.util.errorhandler.DefaultErrorLogger
import io.tokend.template.util.errorhandler.ErrorHandlerFactory
import io.tokend.template.util.errorhandler.ErrorLogger
import io.tokend.template.util.locale.AppLocaleManager
import io.tokend.template.view.ToastManager
import org.tokend.sdk.factory.JsonApiToolsProvider
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

@Module
class UtilModule {
    private var errorHandlerFactory: ErrorHandlerFactory? = null
    private var errorHandlerFactoryLocale: Locale? = null

    @Provides
    fun errorHandlerFactory(
        context: Context,
        localeManager: AppLocaleManager,
        toastManager: ToastManager,
        errorLogger: ErrorLogger
    ): ErrorHandlerFactory {
        val locale = localeManager.getLocale()
        val cached = errorHandlerFactory
        return if (cached != null && errorHandlerFactoryLocale == locale)
            cached
        else
            ErrorHandlerFactory(
                context, //TODO: need to check old variant -> localeManager.getLocalizeContext(context)
                toastManager, errorLogger
            ).also {
                errorHandlerFactory = it
                errorHandlerFactoryLocale = locale
            }
    }

    private var toastManager: ToastManager? = null
    private var toastManagerLocale: Locale? = null

    @Provides
    fun toastManager(
        context: Context,
        localeManager: AppLocaleManager
    ): ToastManager {
        val locale = localeManager.getLocale()
        val cached = toastManager
        return if (cached != null && toastManagerLocale == locale)
            cached
        else
            ToastManager(context).also {   //TODO: need to check old variant -> localeManager.getLocalizeContext(context)
                toastManager = it
                toastManagerLocale = locale
            }
    }

    @Provides
    @Singleton
    fun objectMapper(): ObjectMapper {
        return JsonApiToolsProvider.getObjectMapper()
    }

    @Provides
    @Singleton
    fun errorLogger(): ErrorLogger {
        return DefaultErrorLogger()
    }

    @Provides
    @Singleton
    fun backgroundLockManager(@Named("app") appSharedPreferences: SharedPreferences): BackgroundLockManager {
        return BackgroundLockManager(appSharedPreferences)
    }

    @Provides
    @Singleton
    fun dataCipher(): DataCipher {
        return Aes256GcmDataCipher()
    }

    /*@Provides
    @Singleton
    fun postSignInManagerFactory(apiProvider: ApiProvider,
                                 walletInfoProvider: WalletInfoProvider,
                                 accountProvider: AccountProvider,
                                 repositoryProvider: RepositoryProvider,
                                 connectionStateUtil: ConnectionStateUtil,
                                 session: Session,
                                 errorLogger: ErrorLogger): PostSignInManagerFactory {
        return PostSignInManagerFactory(apiProvider, accountProvider,
            walletInfoProvider, repositoryProvider, connectionStateUtil,
            session, errorLogger)
    }*/

    @Provides
    @Singleton
    fun connectionStateUtil(context: Context): ConnectionStateUtil {
        return ConnectionStateUtil(context)
    }
}