import SwiftUI

struct TransactionView: View {
    @EnvironmentObject private var store: AppStore
    @State private var rows = [TransactionRow()]
    @State private var checking = false
    @State private var submitting = false
    var body: some View {
        Form {
            ForEach($rows) { $row in
                Section {
                    TextField("transaction.asset_code".localized, text: $row.code)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                        .accessibilityIdentifier("transaction.code.\(row.id)")
                        .onChange(of: row.code) { _, _ in row.available = nil }
                    TextField("transaction.asset_name".localized, text: $row.name)
                        .accessibilityIdentifier("transaction.name.\(row.id)")
                        .onChange(of: row.name) { _, _ in row.available = nil }
                    TextField("transaction.value".localized, text: $row.value).keyboardType(.numbersAndPunctuation)
                        .accessibilityIdentifier("transaction.value.\(row.id)")
                        .onChange(of: row.value) { _, _ in row.available = nil }
                    if let available = row.available { AvailabilityBadge(available: available) }
                    Button("transaction.remove".localized, role: .destructive) { rows.removeAll { $0.id == row.id } }
                        .disabled(rows.count == 1).accessibilityIdentifier("transaction.remove.\(row.id)")
                }
            }
            Section {
                Button("transaction.add_item".localized) { rows.append(TransactionRow()) }.accessibilityIdentifier("transaction.add_item")
                Button((checking ? "common.checking" : "assets.check").localized) { check() }
                    .disabled(checking).accessibilityIdentifier("transaction.check")
                Button((submitting ? "transaction.submitting" : "transaction.submit").localized) { submit() }
                    .disabled(submitting).accessibilityIdentifier("transaction.submit")
            }
        }.navigationTitle("transaction.title".localized).accessibilityIdentifier("transaction.content")
    }
    private func check() {
        let codes = rows.map { $0.code.trimmed }.filter { !$0.isEmpty }
        guard !codes.isEmpty else { store.show("transaction.codes_required".localizedString, error: true); return }
        checking = true
        Task {
            defer { checking = false }
            do {
                let results = try await store.client.assets(codes: codes)
                var map: [String: Bool] = [:]
                for result in results { map[result.assetCode] = result.isAssetAvailable }
                for index in rows.indices { rows[index].available = map[rows[index].code] }
            } catch { store.fail(error, fallback: "assets.check_error") }
        }
    }
    private func submit() {
        guard Validation.transactionForm(rows), let items = Validation.transaction(rows) else { store.show("transaction.invalid".localizedString, error: true); return }
        submitting = true
        Task {
            defer { submitting = false }
            do {
                let message = try await store.client.transaction(items: items)
                store.show(message, error: message.contains("Oops"))
            } catch { store.fail(error, fallback: "transaction.error") }
        }
    }
}

struct AvailabilityBadge: View {
    let available: Bool
    var body: some View {
        Text((available ? "assets.available" : "assets.unavailable").localized)
            .font(.caption.bold()).padding(.horizontal, 9).padding(.vertical, 5)
            .foregroundStyle(available ? Color.green : Color.red)
            .background((available ? Color.green : Color.red).opacity(0.12), in: Capsule())
    }
}
