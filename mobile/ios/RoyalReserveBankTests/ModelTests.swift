import XCTest
@testable import RoyalReserveBank

final class ModelTests: XCTestCase {
    @MainActor func testNoticeDismissesAfterDelay() async {
        let waiting = expectation(description: "Timer started")
        var finish: CheckedContinuation<Void, Never>?
        let store = AppStore(settings: AppSettings(), noticeDelay: {
            await withCheckedContinuation { finish = $0; waiting.fulfill() }
        })
        let timer = store.show("Success")
        await fulfillment(of: [waiting])
        XCTAssertEqual(store.notice, "Success")
        finish?.resume()
        await timer.value
        XCTAssertNil(store.notice)
    }
    @MainActor func testNewNoticeResetsTimerAndIgnoresOldCompletion() async {
        let first = expectation(description: "First timer")
        let second = expectation(description: "Replacement timer")
        var timers: [CheckedContinuation<Void, Never>] = []
        let store = AppStore(settings: AppSettings(), noticeDelay: {
            await withCheckedContinuation {
                timers.append($0)
                (timers.count == 1 ? first : second).fulfill()
            }
        })
        let firstTimer = store.show("First")
        await fulfillment(of: [first])
        let secondTimer = store.show("Replacement", error: true)
        await fulfillment(of: [second])
        timers[0].resume()
        await firstTimer.value
        XCTAssertEqual(store.notice, "Replacement")
        XCTAssertTrue(store.noticeIsError)
        timers[1].resume()
        await secondTimer.value
        XCTAssertNil(store.notice)
    }
    @MainActor func testManualNoticeDismissalCancelsPendingTimer() async {
        let waiting = expectation(description: "Timer started")
        var finish: CheckedContinuation<Void, Never>?
        let store = AppStore(settings: AppSettings(), noticeDelay: {
            await withCheckedContinuation { finish = $0; waiting.fulfill() }
        })
        let timer = store.show("Success")
        await fulfillment(of: [waiting])
        store.dismissNotice()
        XCTAssertNil(store.notice)
        finish?.resume()
        await timer.value
        XCTAssertNil(store.notice)
    }
    func testAssetKeyNormalization() throws {
        for (json, expected) in [
            (#"{"assetCode":"X","isAssetAvailable":false,"assetAvailable":true}"#, false),
            (#"{"assetCode":"X","isAssetAvailable":null,"assetAvailable":true}"#, true),
            (#"{"assetCode":"X","assetAvailable":true}"#, true),
            (#"{"assetCode":"X"}"#, false)
        ] {
            XCTAssertEqual(try JSONDecoder().decode(AssetAvailability.self, from: Data(json.utf8)).isAssetAvailable, expected)
        }
    }
    func testCurrencyFormatting() {
        XCTAssertEqual(formatCurrency(12500.5, "USD"), "$12,500.50")
        XCTAssertEqual(formatCurrency(3200, "EUR"), "€3,200.00")
        XCTAssertEqual(formatCurrency(1.2, "INVALID"), "INVALID 1.20")
    }
    func testAccountValidation() {
        XCTAssertEqual(Validation.account(" Jane ", "0"), 0)
        XCTAssertEqual(Validation.account("Jane", "12.50"), 12.5)
        for pair in [(" ", "0"), ("Jane", ""), ("Jane", "NaN"), ("Jane", "-1"), ("Jane", "inf")] {
            XCTAssertNil(Validation.account(pair.0, pair.1))
        }
    }
    func testDepositValidation() {
        XCTAssertEqual(Validation.deposit("0.01"), 0.01)
        for value in ["", "0", "-1", "nan", "inf", "x"] { XCTAssertNil(Validation.deposit(value)) }
    }
    func testMoneyInputMatchesNumericMinAndStepConstraints() {
        XCTAssertTrue(Validation.moneyInput("0", minimum: 0))
        XCTAssertTrue(Validation.moneyInput("12.34", minimum: 0.01))
        for value in ["", "NaN", "0", "-1", "0.001", "1.999"] {
            XCTAssertFalse(Validation.moneyInput(value, minimum: 0.01))
        }
    }
    func testTransactionFormConstraintsAreSeparateFromParseIntHandler() {
        XCTAssertTrue(Validation.transactionForm([TransactionRow(code: "AAPL", value: "1e3")]))
        XCTAssertFalse(Validation.transactionForm([]))
        XCTAssertFalse(Validation.transactionForm([TransactionRow()]))
        for value in ["", "0", "1.5", "12abc", "nan"] {
            XCTAssertFalse(Validation.transactionForm([TransactionRow(code: "AAPL", value: value)]))
        }
    }
    func testParseIntSemantics() {
        XCTAssertEqual(Validation.parseInt("  +12.9"), 12)
        XCTAssertEqual(Validation.parseInt("1e3"), 1)
        XCTAssertEqual(Validation.parseInt("12abc"), 12)
        XCTAssertEqual(Validation.parseInt("-2.5"), -2)
        XCTAssertEqual(Validation.parseInt("0x10"), 0)
        XCTAssertNil(Validation.parseInt("abc12"))
        XCTAssertNil(Validation.parseInt(""))
    }
    func testTransactionValidationAndTrimming() {
        let parsed = Validation.transaction([TransactionRow(code: " PETR4 ", name: " Name ", value: "2.5"), TransactionRow()])
        XCTAssertEqual(parsed, [TransactionItem(assetCode: "PETR4", assetName: "Name", value: 2)])
        XCTAssertNil(Validation.transaction([]))
        XCTAssertNil(Validation.transaction([TransactionRow()]))
        for value in ["0", "-1", "", "bad"] { XCTAssertNil(Validation.transaction([TransactionRow(code: "AAPL", value: value)])) }
    }
    func testAssetSplittingPreservesCaseDuplicatesAndOrder() {
        XCTAssertEqual(Validation.codes(" PETR4,\nAAPL\tPETR4 ,, aapl "), ["PETR4", "AAPL", "PETR4", "aapl"])
        XCTAssertEqual(Validation.codes(" , \n"), [])
    }
    @MainActor func testSettingsPersistenceNormalizationAndClear() {
        let name = "test.\(UUID())"
        let defaults = UserDefaults(suiteName: name)!
        defer { defaults.removePersistentDomain(forName: name) }
        let settings = AppSettings(defaults: defaults)
        XCTAssertTrue(settings.mockMode)
        XCTAssertEqual(settings.baseURL, "http://localhost:8080")
        settings.mockMode = false
        settings.saveBaseURL(" http://localhost:9000/// \n")
        settings.saveToken(" token \n")
        let loaded = AppSettings(defaults: defaults)
        XCTAssertFalse(loaded.mockMode)
        XCTAssertEqual(loaded.baseURL, "http://localhost:9000")
        XCTAssertEqual(loaded.token, "token")
        loaded.clearToken()
        XCTAssertNil(defaults.object(forKey: "rrb.token"))
    }
    @MainActor func testSharedStoreRefreshesAfterMutationAndSwitchesImmediately() async throws {
        let name = "test.\(UUID())"
        let defaults = UserDefaults(suiteName: name)!
        defer { defaults.removePersistentDomain(forName: name) }
        let settings = AppSettings(defaults: defaults)
        let store = AppStore(settings: settings)
        await store.refresh()
        XCTAssertEqual(store.accounts.count, 3)
        _ = try await store.client.create(holder: "Jane", balance: 1, currency: "USD")
        await store.refresh()
        XCTAssertEqual(store.accounts.count, 4)
        settings.mockMode = false
        XCTAssertTrue(store.client is RemoteAPIClient)
        settings.mockMode = true
        XCTAssertTrue(store.client is MockAPIClient)
    }
}
