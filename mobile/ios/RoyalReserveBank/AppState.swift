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
    @Published private(set) var notice: String?
    @Published var noticeIsError = false
    private var refreshID = UUID()
    private var noticeTask: Task<Void, Never>?
    private let noticeDelay: @MainActor () async throws -> Void
    var client: any APIClient {
        settings.mockMode ? mock : RemoteAPIClient(baseURL: settings.baseURL, token: settings.token)
    }
    init(settings: AppSettings, noticeDelay: @escaping @MainActor () async throws -> Void = {
        try await Task.sleep(for: .seconds(5))
    }) {
        self.settings = settings
        self.noticeDelay = noticeDelay
    }
    deinit { noticeTask?.cancel() }
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
    @discardableResult
    func show(_ text: String, error: Bool = false) -> Task<Void, Never> {
        noticeTask?.cancel()
        notice = text
        noticeIsError = error
        let task = Task { [weak self, noticeDelay] in
            do {
                try await noticeDelay()
                try Task.checkCancellation()
                self?.dismissNotice()
            } catch {}
        }
        noticeTask = task
        return task
    }
    func dismissNotice() {
        noticeTask?.cancel()
        noticeTask = nil
        notice = nil
    }
    func fail(_ error: Error, fallback: String) { show((error as? APIError)?.body ?? fallback.localizedString, error: true) }
}
