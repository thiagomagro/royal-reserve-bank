import SwiftUI

struct AccountsView: View {
    @EnvironmentObject private var store: AppStore
    @State private var creating = false
    @State private var holder = ""
    @State private var balance = "0"
    @State private var currency = "USD"
    @State private var target: Account?
    @State private var amount = ""
    @State private var pendingDelete: Account?
    @State private var busy = false
    @State private var sheetError: String?
    @FocusState private var amountFocused: Bool
    var body: some View {
        List {
            AccountQueryState(dashboard: false)
            if !store.loading && store.loadError == nil {
                AccountTableHeader()
                ForEach(store.accounts) { account in
                    VStack(alignment: .leading, spacing: 10) {
                        AccountSummary(account: account)
                        LabeledContent("accounts.currency".localized, value: account.currency).font(.caption)
                        HStack {
                            Button("accounts.add_funds".localized) { amount = ""; sheetError = nil; target = account }
                                .accessibilityIdentifier("accounts.deposit.\(account.id)")
                            Spacer()
                            Button("common.delete".localized, role: .destructive) { pendingDelete = account }
                                .accessibilityIdentifier("accounts.delete.\(account.id)")
                        }.buttonStyle(.bordered)
                    }.padding(.vertical, 6)
                }
            }
        }.navigationTitle("accounts.title".localized).accessibilityIdentifier("accounts.content")
            .toolbar { Button("accounts.new.title".localized) { sheetError = nil; creating = true }.accessibilityIdentifier("accounts.new") }
            .refreshable { await store.refresh() }
            .sheet(isPresented: $creating) { createSheet }
            .sheet(item: $target, onDismiss: { amount = "" }) { account in depositSheet(account) }
            .alert("accounts.delete_title".localized, isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } })) {
                Button("common.cancel".localized, role: .cancel) { pendingDelete = nil }
                Button("common.delete".localized, role: .destructive) {
                    guard let account = pendingDelete else { return }
                    pendingDelete = nil
                    Task {
                        do { store.show(try await store.client.delete(holder: account.accountHolderName)); await store.refresh() }
                        catch { store.fail(error, fallback: "accounts.delete_error") }
                    }
                }.accessibilityIdentifier("accounts.delete_confirm")
            } message: { Text("accounts.delete_message".formatted(pendingDelete?.accountHolderName ?? "")) }
    }
    private var createSheet: some View {
        NavigationStack {
            Form {
                if let sheetError { Text(sheetError).foregroundStyle(.red) }
                TextField("accounts.holder_name".localized, text: $holder).accessibilityIdentifier("accounts.holder")
                TextField("accounts.initial_balance".localized, text: $balance).keyboardType(.decimalPad).accessibilityIdentifier("accounts.balance")
                Picker("accounts.currency".localized, selection: $currency) {
                    ForEach(["USD", "EUR", "GBP", "BRL"], id: \.self) { Text($0).tag($0) }
                }.accessibilityIdentifier("accounts.currency")
                Button((busy ? "accounts.creating" : "accounts.create").localized) { create() }
                    .disabled(busy).accessibilityIdentifier("accounts.create")
            }.navigationTitle("accounts.new.title".localized)
                .toolbar { Button("common.cancel".localized) { creating = false }.disabled(busy) }
                .interactiveDismissDisabled(busy)
        }
    }
    private func depositSheet(_ account: Account) -> some View {
        NavigationStack {
            Form {
                Text("accounts.current_balance".formatted(account.accountHolderName, formatCurrency(account.balance, account.currency)))
                if let sheetError { Text(sheetError).foregroundStyle(.red) }
                TextField("accounts.amount".formatted(account.currency), text: $amount)
                    .keyboardType(.decimalPad).focused($amountFocused).accessibilityIdentifier("accounts.deposit_amount")
                Button((busy ? "accounts.adding" : "accounts.confirm_deposit").localized) { deposit(account) }
                    .disabled(busy).accessibilityIdentifier("accounts.confirm_deposit")
            }.navigationTitle("accounts.add_funds".localized)
                .toolbar { Button("common.cancel".localized) { target = nil; amount = "" }.disabled(busy) }
                .interactiveDismissDisabled(busy).onAppear { amountFocused = true }
        }
    }
    private func create() {
        guard Validation.moneyInput(balance, minimum: 0), let value = Validation.account(holder, balance) else {
            sheetError = "accounts.invalid".localizedString; return
        }
        busy = true
        Task {
            defer { busy = false }
            do {
                let message = try await store.client.create(holder: holder.trimmed, balance: value, currency: currency)
                creating = false; holder = ""; balance = "0"; currency = "USD"
                store.show(message); await store.refresh()
            } catch { sheetError = (error as? APIError)?.body ?? "accounts.create_error".localizedString }
        }
    }
    private func deposit(_ account: Account) {
        guard Validation.moneyInput(amount, minimum: 0.01), let value = Validation.deposit(amount) else { sheetError = "accounts.deposit_invalid".localizedString; return }
        busy = true
        Task {
            defer { busy = false }
            do {
                let updated = try await store.client.deposit(id: account.id, amount: value)
                target = nil; amount = ""
                store.show("accounts.deposit_success".formatted(updated.accountHolderName, formatCurrency(updated.balance, updated.currency)))
                await store.refresh()
            } catch { sheetError = (error as? APIError)?.body ?? "accounts.deposit_error".localizedString }
        }
    }
}
