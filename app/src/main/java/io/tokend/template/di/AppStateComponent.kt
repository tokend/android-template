package io.tokend.template.di

import dagger.Component
import io.tokend.template.base.activity.BaseActivity
import io.tokend.template.base.fragment.BaseFragment
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        /*AccountProviderModule::class,
        WalletInfoProviderModule::class,
        AppTfaCallbackModule::class,
        ApiProviderModule::class,
        RepositoriesModule::class,*/
        PersistenceModule::class,
        /*UrlConfigProviderModule::class,
        UtilModule::class,*/
        AppModule::class,
//    SessionModule::class,
        LocaleManagerModule::class,
//    AppDatabaseModule::class
    ]
)
interface AppStateComponent {
    fun inject(baseActivity: BaseActivity)
    fun inject(baseFragment: BaseFragment)
}