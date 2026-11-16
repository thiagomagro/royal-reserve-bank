import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var settings: AppSettings
    @State private var baseURL = ""
    @State private var token = ""
    var body: some View {
        Form {
            Section {
                Toggle("settings.mock_toggle".localized, isOn: $settings.mockMode).accessibilityIdentifier("settings.mock_mode")
                Text((settings.mockMode ? "settings.mock_enabled" : "settings.mock_disabled").localized)
            }
            Section("settings.base_url".localized) {
                TextField("settings.base_url".localized, text: $baseURL)
                    .keyboardType(.URL).textInputAutocapitalization(.never).autocorrectionDisabled()
                    .accessibilityIdentifier("settings.base_url")
                Button("settings.save_url".localized) {
                    settings.saveBaseURL(baseURL); baseURL = settings.baseURL
                    store.show("settings.url_saved".localizedString)
                }.accessibilityIdentifier("settings.save_url")
            }
            Section {
                Text("settings.token".localized)
                TextEditor(text: $token).font(.system(.caption, design: .monospaced)).frame(minHeight: 110)
                    .textInputAutocapitalization(.never).autocorrectionDisabled().accessibilityIdentifier("settings.token")
                Text("settings.token_note".localized).font(.caption).foregroundStyle(.secondary)
                Button("settings.save_token".localized) {
                    settings.saveToken(token); token = settings.token; store.show("settings.token_saved".localizedString)
                }.accessibilityIdentifier("settings.save_token")
                Button("settings.clear_token".localized) {
                    settings.clearToken(); token = ""; store.show("settings.token_cleared".localizedString)
                }.accessibilityIdentifier("settings.clear_token")
            }
        }.navigationTitle("settings.title".localized).accessibilityIdentifier("settings.content")
            .onAppear { baseURL = settings.baseURL; token = settings.token }
    }
}
