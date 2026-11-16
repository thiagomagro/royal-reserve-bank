import XCTest
@testable import RoyalReserveBank

final class StubProtocol: URLProtocol {
    static var handler: ((URLRequest) throws -> (Int, String))!
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        do {
            let (status, body) = try Self.handler(request)
            client?.urlProtocol(self, didReceive: HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: nil, headerFields: nil)!, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: Data(body.utf8))
            client?.urlProtocolDidFinishLoading(self)
        } catch { client?.urlProtocol(self, didFailWithError: error) }
    }
    override func stopLoading() {}
}

final class RemoteAPIClientTests: XCTestCase {
    private func client(token: String = "") -> RemoteAPIClient {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [StubProtocol.self]
        return RemoteAPIClient(baseURL: "http://localhost:8080", token: token, session: URLSession(configuration: config))
    }
    private func body(_ request: URLRequest) throws -> [String: Any] {
        if let data = request.httpBody { return try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any]) }
        let stream = try XCTUnwrap(request.httpBodyStream)
        stream.open(); defer { stream.close() }
        var data = Data(); var buffer = [UInt8](repeating: 0, count: 1024)
        while stream.hasBytesAvailable {
            let count = stream.read(&buffer, maxLength: buffer.count)
            if count <= 0 { break }; data.append(buffer, count: count)
        }
        return try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
    }
    func testGetNoAuthNoBodyAndEmptyList() async throws {
        StubProtocol.handler = { request in
            XCTAssertEqual(request.httpMethod, "GET")
            XCTAssertEqual(request.url?.path, "/api/account")
            XCTAssertNil(request.value(forHTTPHeaderField: "Authorization"))
            XCTAssertNil(request.value(forHTTPHeaderField: "Content-Type"))
            XCTAssertNil(request.httpBodyStream)
            return (200, "[]")
        }
        let list = try await client().accounts()
        XCTAssertTrue(list.isEmpty)
    }
    func testCreateJSONAuthAndPlainText201() async throws {
        StubProtocol.handler = { request in
            XCTAssertEqual(request.httpMethod, "POST")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer jwt")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/json")
            let body = try self.body(request)
            XCTAssertEqual(body["accountHolderName"] as? String, "Jane")
            XCTAssertEqual(body["balance"] as? Double, 25.5)
            XCTAssertEqual(body["currency"] as? String, "USD")
            return (201, "Created raw text")
        }
        let message = try await client(token: "jwt").create(holder: "Jane", balance: 25.5, currency: "USD")
        XCTAssertEqual(message, "Created raw text")
    }
    func testDeleteHasJSONBody() async throws {
        StubProtocol.handler = { request in
            XCTAssertEqual(request.httpMethod, "DELETE")
            XCTAssertEqual(request.url?.path, "/api/account")
            XCTAssertEqual(try self.body(request)["accountHolderName"] as? String, "Jane")
            return (200, "Deleted")
        }
        let message = try await client().delete(holder: "Jane")
        XCTAssertEqual(message, "Deleted")
    }
    func testDepositEncodesOnePathComponentAndJSONAmount() async throws {
        StubProtocol.handler = { request in
            XCTAssertEqual(request.url?.absoluteString, "http://localhost:8080/api/account/a%2Fb%20%3F%23%25/deposit")
            XCTAssertEqual(request.httpMethod, "POST")
            XCTAssertEqual(try self.body(request)["amount"] as? Double, 1.5)
            return (200, #"{"id":"a/b ?#%","accountNumber":"1001","accountHolderName":"Jane","balance":1.5,"currency":"USD"}"#)
        }
        let result = try await client().deposit(id: "a/b ?#%", amount: 1.5)
        XCTAssertEqual(result.balance, 1.5)
    }
    func testAssetsRepeatedQueriesOrderAndEncoding() async throws {
        StubProtocol.handler = { request in
            let items = URLComponents(url: request.url!, resolvingAgainstBaseURL: false)?.queryItems
            XCTAssertEqual(items?.map(\.name), ["assetCode", "assetCode", "assetCode"])
            XCTAssertEqual(items?.compactMap(\.value), ["A B", "A&B", "A B"])
            return (200, "[]")
        }
        _ = try await client().assets(codes: ["A B", "A&B", "A B"])
    }
    func testTransactionPayloadAndOopsIsTransportSuccess() async throws {
        StubProtocol.handler = { request in
            XCTAssertEqual(request.url?.path, "/api/transaction")
            let rows = try self.body(request)["transactionItemsDtoList"] as? [[String: Any]]
            XCTAssertEqual(rows?.first?["value"] as? Int, 2)
            XCTAssertEqual(rows?.first?["assetCode"] as? String, "X")
            XCTAssertNil(rows?.first?["id"])
            return (201, "Oops! Something went wrong, please try again later!")
        }
        let result = try await client().transaction(items: [TransactionItem(assetCode: "X", assetName: "Name", value: 2)])
        XCTAssertTrue(result.contains("Oops"))
    }
    func testRawErrorBodiesAndEmptyStatusFallback() async throws {
        for (status, text) in [(401, "Unauthorized raw"), (400, #"{"message":"keep raw"}"#), (404, "")] {
            StubProtocol.handler = { _ in (status, text) }
            do { _ = try await client().accounts(); XCTFail() }
            catch let error as APIError {
                XCTAssertEqual(error.status, status)
                XCTAssertEqual(error.body, text.isEmpty ? HTTPURLResponse.localizedString(forStatusCode: status) : text)
            }
        }
    }
    func testNetworkAndDecodeFailuresAreNotAPIErrors() async throws {
        StubProtocol.handler = { _ in throw URLError(.notConnectedToInternet) }
        do { _ = try await client().accounts(); XCTFail() } catch { XCTAssertFalse(error is APIError) }
        StubProtocol.handler = { _ in (200, "not JSON") }
        do { _ = try await client().accounts(); XCTFail() } catch { XCTAssertTrue(error is DecodingError) }
    }
    func testAny2xxTextSuccess() async throws {
        StubProtocol.handler = { _ in (202, "Accepted") }
        let accepted = try await client().delete(holder: "Jane")
        XCTAssertEqual(accepted, "Accepted")
        StubProtocol.handler = { _ in (204, "") }
        let empty = try await client().delete(holder: "Jane")
        XCTAssertEqual(empty, "")
    }
}
