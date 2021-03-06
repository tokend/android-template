package io.tokend.template.logic.providers

import io.tokend.template.logic.credentials.model.WalletInfoRecord

class WalletInfoProviderImpl : WalletInfoProvider {
    private var walletInfo: WalletInfoRecord? = null

    override fun setWalletInfo(walletInfo: WalletInfoRecord?) {
        this.walletInfo = walletInfo
    }

    override fun getWalletInfo(): WalletInfoRecord? {
        return walletInfo
    }
}