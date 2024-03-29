package io.tokend.template.logic.providers

import org.tokend.wallet.Account

class AccountProviderImpl : AccountProvider {
    private var accounts: List<Account> = emptyList()

    override fun getDefaultAccount(): Account {
        return accounts.firstOrNull()
            ?: throw NoSuchElementException("The list of accounts is empty")
    }

    override fun getAccount(accountId: String): Account? {
        return accounts.find { it.accountId == accountId }
    }

    override fun getAccounts(): List<Account> {
        return accounts.toList()
    }

    override fun setAccounts(accounts: List<Account>) {
        this.accounts = accounts
    }
}