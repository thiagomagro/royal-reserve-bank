import XCTest
@testable import RoyalReserveBank

final class MockAPIClientTests: XCTestCase {
    func testSeedsAndFreshStore() async throws {
        let accounts = await MockAPIClient().accounts()
        XCTAssertEqual(accounts.map(\.id), ["1", "2", "3"])
        XCTAssertEqual(accounts.map(\.accountNumber), ["1001", "1002", "1003"])
        XCTAssertEqual(accounts.map(\.accountHolderName), ["Alice Johnson", "Bruno Souza", "Carla Mendes"])
        XCTAssertEqual(accounts.map(\.balance), [12500.5, 8400, 3200])
        XCTAssertEqual(accounts.map(\.currency), ["USD", "BRL", "EUR"])
    }
    func testCreateCountersAndNoReuse() async throws {
        let client = MockAPIClient()
        let message = await client.create(holder: "Jane", balance: 0, currency: "GBP")
        XCTAssertEqual(message, "Successfully set up a new bank account for Jane.")
        _ = try await client.delete(holder: "Jane")
        _ = await client.create(holder: "Jane", balance: 1, currency: "EUR")
        let last = await client.accounts().last
        XCTAssertEqual(last?.id, "5")
        XCTAssertEqual(last?.accountNumber, "1005")
        XCTAssertEqual(last?.balance, 1)
    }
    func testDeleteFirstExactMatchAndEmptyList() async throws {
        let client = MockAPIClient()
        _ = await client.create(holder: "Alice Johnson", balance: 1, currency: "USD")
        let message = try await client.delete(holder: "Alice Johnson")
        XCTAssertEqual(message, "Successfully deleted the bank account for Alice Johnson.")
        let list = await client.accounts()
        XCTAssertEqual(list.map(\.id), ["2", "3", "4"])
        for account in list { _ = try await client.delete(holder: account.accountHolderName) }
        let empty = await client.accounts()
        XCTAssertTrue(empty.isEmpty)
    }
    func testDeleteMissingAndCaseSensitive() async throws {
        do { _ = try await MockAPIClient().delete(holder: "alice johnson"); XCTFail() }
        catch let error as APIError {
            XCTAssertEqual(error.status, 404)
            XCTAssertEqual(error.body, "No bank account found for alice johnson.")
        }
    }
    func testDepositRounding() async throws {
        let client = MockAPIClient()
        let account = try await client.deposit(id: "1", amount: 25.505)
        XCTAssertEqual(account.balance, 12526.01)
        let stored = await client.accounts().first
        XCTAssertEqual(stored, account)
    }
    func testDepositValidationBeforeLookup() async throws {
        for value in [0, -1, Double.nan] {
            do { _ = try await MockAPIClient().deposit(id: "missing", amount: value); XCTFail() }
            catch let error as APIError {
                XCTAssertEqual(error.status, 400)
                XCTAssertEqual(error.body, "The deposit amount must be greater than zero.")
            }
        }
    }
    func testDepositMissing() async throws {
        do { _ = try await MockAPIClient().deposit(id: "missing", amount: 1); XCTFail() }
        catch let error as APIError {
            XCTAssertEqual(error.status, 404)
            XCTAssertEqual(error.body, "No bank account found with id missing.")
        }
    }
    func testTransactionSuccessEmptyAndOopsWithoutMutation() async throws {
        let client = MockAPIClient()
        let original = await client.accounts()
        let empty = await client.transaction(items: [])
        XCTAssertEqual(empty, "Transaction placed successfully.")
        let success = await client.transaction(items: [TransactionItem(assetCode: "PETR4", assetName: "", value: 1)])
        XCTAssertEqual(success, empty)
        let failure = await client.transaction(items: [TransactionItem(assetCode: "petr4", assetName: "", value: 1)])
        XCTAssertEqual(failure, "Oops! Something went wrong, please try again later!")
        let after = await client.accounts()
        XCTAssertEqual(after, original)
    }
    func testAssetsOrderDuplicatesUnknownAndEmpty() async throws {
        let client = MockAPIClient()
        let codes = ["AAPL", "UNKNOWN", "PETR4", "AAPL", "VALE3", "BOND-BR-2030", "aapl"]
        let results = await client.assets(codes: codes)
        XCTAssertEqual(results.map(\.assetCode), codes)
        XCTAssertEqual(results.map(\.isAssetAvailable), [true, false, true, true, true, true, false])
        let empty = await client.assets(codes: [])
        XCTAssertTrue(empty.isEmpty)
    }
}
