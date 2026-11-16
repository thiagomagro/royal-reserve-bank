import SwiftUI

@MainActor
final class AppSettings: ObservableObject {
    private let defaults: UserDefaults
    @Published var mockMode: Bool { didSet { defaults.set(mockMode, forKey: "rrb.mock") } }
    @Published var baseURL: String { didSet { defaults.set(baseURL, forKey: "rrb.base_url") } }
    @Published private(set) var token: String
    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        mockMode = defaults.object(forKey: "rrb.mock") as? Bool ?? true
        baseURL = defaults.string(forKey: "rrb.base_url") ?? "http://localhost:8080"
        token = defaults.string(forKey: "rrb.token") ?? ""
    }
    func saveBaseURL(_ input: String) {
        var value = input.trimmed
        while value.hasSuffix("/") { value.removeLast() }
        baseURL = value
    }
    func saveToken(_ input: String) { token = input.trimmed; defaults.set(token, forKey: "rrb.token") }
    func clearToken() { token = ""; defaults.removeObject(forKey: "rrb.token") }
}

@MainActor
final class AppStore: ObservableObject {
    let settings: AppSettings
    let mock = MockAPIClient()
    @Published var accounts: [Account] = []
    @Published var loading = false
    @Published var loadError: String?
    @Published var notice: String?
    @Published var noticeIsError = false
    private var refreshID = UUID()
    var client: any APIClient {
        settings.mockMode ? mock : RemoteAPIClient(baseURL: settings.baseURL, token: settings.token)
    }
    init(settings: AppSettings) { self.settings = settings }
    func refresh() async {
        let id = UUID(); refreshID = id
        loading = true; loadError = nil
        do {
            let result = try await client.accounts()
            guard id == refreshID else { return }
            accounts = result
        } catch {
            guard id == refreshID else { return }
            loadError = "accounts.load_error".formatted(error.localizedDescription)
        }
        loading = false
    }
    func show(_ text: String, error: Bool = false) { notice = text; noticeIsError = error }
    func fail(_ error: Error, fallback: String) { show((error as? APIError)?.body ?? fallback.localizedString, error: true) }
}
