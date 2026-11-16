import SwiftUI

@main
struct RoyalReserveBankApp: App {
    @StateObject private var settings: AppSettings
    @StateObject private var store: AppStore
    init() {
        let settings = AppSettings()
        _settings = StateObject(wrappedValue: settings)
        _store = StateObject(wrappedValue: AppStore(settings: settings))
    }
    var body: some Scene {
        WindowGroup {
            RootView().environmentObject(settings).environmentObject(store)
                .tint(Color(red: 0.06, green: 0.09, blue: 0.16))
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var settings: AppSettings
    @State private var tab = 0
    var body: some View {
        TabView(selection: $tab) {
            NavigationStack { DashboardView(tab: $tab) }
                .tabItem { Label("dashboard.title".localized, systemImage: "square.grid.2x2") }.tag(0)
            NavigationStack { AccountsView() }
                .tabItem { Label("accounts.title".localized, systemImage: "person.2") }.tag(1)
            NavigationStack { TransactionView() }
                .tabItem { Label("transaction.tab".localized, systemImage: "arrow.left.arrow.right") }.tag(2)
            NavigationStack { AssetsView() }
                .tabItem { Label("assets.title".localized, systemImage: "chart.bar") }.tag(3)
            NavigationStack { SettingsView() }
                .tabItem { Label("settings.title".localized, systemImage: "gearshape") }.tag(4)
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            VStack(spacing: 0) {
                HStack(spacing: 10) {
                    Image(systemName: "building.columns.fill").foregroundStyle(.yellow)
                    Text("app.brand".localized).font(.headline)
                    Spacer()
                }.padding().foregroundStyle(.white).background(Color(red: 0.06, green: 0.09, blue: 0.16))
                if let notice = store.notice {
                    HStack {
                        Image(systemName: store.noticeIsError ? "exclamationmark.circle" : "checkmark.circle")
                        Text(notice).font(.subheadline)
                        Spacer()
                        Button { store.dismissNotice() } label: { Image(systemName: "xmark") }
                            .accessibilityLabel("common.dismiss".localized).accessibilityIdentifier("notice.dismiss")
                    }.padding().foregroundStyle(store.noticeIsError ? Color.red : Color.green)
                        .background(.regularMaterial).accessibilityIdentifier("notice.banner")
                }
            }
        }
        .task { await store.refresh() }
        .onChange(of: settings.mockMode) { _, _ in Task { await store.refresh() } }
        .onChange(of: settings.baseURL) { _, _ in Task { await store.refresh() } }
        .onChange(of: settings.token) { _, _ in Task { await store.refresh() } }
    }
}

struct AccountQueryState: View {
    @EnvironmentObject var store: AppStore
    let dashboard: Bool
    var body: some View {
        if store.loading { ProgressView((dashboard ? "dashboard.loading" : "accounts.loading").localizedString) }
        if let error = store.loadError { Text(error).foregroundStyle(.red) }
    }
}

struct DashboardView: View {
    @EnvironmentObject private var store: AppStore
    @Binding var tab: Int
    private var currencies: [String] { store.accounts.reduce(into: [String]()) { if !$0.contains($1.currency) { $0.append($1.currency) } } }
    var body: some View {
        List {
            AccountQueryState(dashboard: true)
            if !store.loading && store.loadError == nil {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("accounts.title".localized).foregroundStyle(.secondary)
                        Text(store.accounts.count, format: .number).font(.largeTitle.bold())
                    }.padding(.vertical, 6)
                    ForEach(currencies, id: \.self) { currency in
                        VStack(alignment: .leading, spacing: 6) {
                            Text("dashboard.total".formatted(currency)).foregroundStyle(.secondary)
                            Text(formatCurrency(store.accounts.filter { $0.currency == currency }.reduce(0) { $0 + $1.balance }, currency))
                                .font(.title2.bold())
                        }.padding(.vertical, 5)
                    }
                }
                Section("dashboard.top".localized) {
                    AccountTableHeader()
                    ForEach(Array(store.accounts.sorted { $0.balance > $1.balance }.prefix(5))) { account in
                        AccountSummary(account: account)
                    }
                }
                Section {
                    Button("dashboard.manage".localized) { tab = 1 }.accessibilityIdentifier("dashboard.manage_accounts")
                    Button("transaction.title".localized) { tab = 2 }.accessibilityIdentifier("dashboard.new_transaction")
                    Button("dashboard.check_assets".localized) { tab = 3 }.accessibilityIdentifier("dashboard.check_assets")
                }
            }
        }.navigationTitle("dashboard.title".localized).accessibilityIdentifier("dashboard.content")
            .refreshable { await store.refresh() }
    }
}

struct AccountTableHeader: View {
    var body: some View {
        HStack {
            Text("accounts.holder".localized)
            Spacer()
            Text("accounts.number".localized)
            Spacer()
            Text("accounts.balance".localized)
        }.font(.caption).foregroundStyle(.secondary)
    }
}

struct AccountSummary: View {
    let account: Account
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline) {
                Text(account.accountHolderName).font(.headline)
                Spacer()
                Text(formatCurrency(account.balance, account.currency)).font(.subheadline.bold())
            }
            Text(account.accountNumber).font(.caption).foregroundStyle(.secondary)
        }.padding(.vertical, 4).accessibilityIdentifier("account.row.\(account.id)")
    }
}
