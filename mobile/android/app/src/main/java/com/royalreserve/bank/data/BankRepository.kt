package com.royalreserve.bank.data

class BankRepository(
    private val settings: SettingsRepository,
    private val mock: BankApi = MockBankApi(),
) {
    fun api(): BankApi =
        if (settings.mockMode) mock else HttpBankApi(settings.baseUrl, settings.token)
}
