import SwiftUI

struct AssetsView: View {
    @EnvironmentObject private var store: AppStore
    @State private var input = ""
    @State private var results: [AssetAvailability] = []
    @State private var checking = false
    var body: some View {
        Form {
            TextField("assets.placeholder".localized, text: $input)
                .textInputAutocapitalization(.never).autocorrectionDisabled().accessibilityIdentifier("assets.input")
            Button((checking ? "common.checking" : "assets.check").localized) { check() }
                .disabled(checking).accessibilityIdentifier("assets.check")
            if !results.isEmpty {
                Section {
                    HStack { Text("transaction.asset_code".localized); Spacer(); Text("assets.status".localized) }.foregroundStyle(.secondary)
                    ForEach(Array(results.enumerated()), id: \.offset) { index, result in
                        HStack { Text(result.assetCode); Spacer(); AvailabilityBadge(available: result.isAssetAvailable) }
                            .accessibilityIdentifier("assets.result.\(index)")
                    }
                }
            }
        }.navigationTitle("assets.title".localized).accessibilityIdentifier("assets.content")
    }
    private func check() {
        let codes = Validation.codes(input)
        guard !codes.isEmpty else { store.show("assets.required".localizedString, error: true); return }
        checking = true
        Task {
            defer { checking = false }
            do { results = try await store.client.assets(codes: codes) }
            catch { store.fail(error, fallback: "assets.check_error") }
        }
    }
}
