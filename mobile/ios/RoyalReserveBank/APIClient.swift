import Foundation

struct APIError: LocalizedError {
    let status: Int
    let body: String
    var errorDescription: String? { body }
}

protocol APIClient {
    func accounts() async throws -> [Account]
    func create(holder: String, balance: Double, currency: String) async throws -> String
    func delete(holder: String) async throws -> String
    func deposit(id: String, amount: Double) async throws -> Account
    func transaction(items: [TransactionItem]) async throws -> String
    func assets(codes: [String]) async throws -> [AssetAvailability]
}

struct RemoteAPIClient: APIClient {
    let baseURL: String
    let token: String
    var session: URLSession = .shared

    func request(_ path: String, method: String = "GET", body: Data? = nil,
                 query: [URLQueryItem] = []) async throws -> Data {
        guard var components = URLComponents(string: baseURL + path) else { throw URLError(.badURL) }
        if !query.isEmpty { components.queryItems = query }
        guard let url = components.url else { throw URLError(.badURL) }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.httpBody = body
        if body != nil { request.setValue("application/json", forHTTPHeaderField: "Content-Type") }
        if !token.isEmpty { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        guard (200..<300).contains(http.statusCode) else {
            let text = String(decoding: data, as: UTF8.self)
            throw APIError(status: http.statusCode, body: text.isEmpty ? HTTPURLResponse.localizedString(forStatusCode: http.statusCode) : text)
        }
        return data
    }
    private func json(_ value: [String: Any]) throws -> Data { try JSONSerialization.data(withJSONObject: value) }
    func accounts() async throws -> [Account] {
        try JSONDecoder().decode([Account].self, from: await request("/api/account"))
    }
    func create(holder: String, balance: Double, currency: String) async throws -> String {
        let body = try json(["accountHolderName": holder, "balance": balance, "currency": currency])
        return String(decoding: try await request("/api/account", method: "POST", body: body), as: UTF8.self)
    }
    func delete(holder: String) async throws -> String {
        String(decoding: try await request("/api/account", method: "DELETE", body: json(["accountHolderName": holder])), as: UTF8.self)
    }
    func deposit(id: String, amount: Double) async throws -> Account {
        let encoded = id.addingPercentEncoding(withAllowedCharacters: CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-._~")))!
        return try JSONDecoder().decode(Account.self, from: await request("/api/account/\(encoded)/deposit", method: "POST", body: json(["amount": amount])))
    }
    func transaction(items: [TransactionItem]) async throws -> String {
        struct Body: Encodable { let transactionItemsDtoList: [TransactionItem] }
        return String(decoding: try await request("/api/transaction", method: "POST", body: JSONEncoder().encode(Body(transactionItemsDtoList: items))), as: UTF8.self)
    }
    func assets(codes: [String]) async throws -> [AssetAvailability] {
        try JSONDecoder().decode([AssetAvailability].self, from: await request("/api/asset-management", query: codes.map { URLQueryItem(name: "assetCode", value: $0) }))
    }
}

actor MockAPIClient: APIClient {
    private var list = [
        Account(id: "1", accountNumber: "1001", accountHolderName: "Alice Johnson", balance: 12500.5, currency: "USD"),
        Account(id: "2", accountNumber: "1002", accountHolderName: "Bruno Souza", balance: 8400, currency: "BRL"),
        Account(id: "3", accountNumber: "1003", accountHolderName: "Carla Mendes", balance: 3200, currency: "EUR")
    ]
    private var nextID = 4
    private var nextNumber = 1004
    private let available: Set<String> = ["PETR4", "VALE3", "AAPL", "BOND-BR-2030"]
    func accounts() -> [Account] { list }
    func create(holder: String, balance: Double, currency: String) -> String {
        list.append(Account(id: String(nextID), accountNumber: String(nextNumber), accountHolderName: holder, balance: balance, currency: currency))
        nextID += 1
        nextNumber += 1
        return "mock.created".formatted(holder)
    }
    func delete(holder: String) throws -> String {
        guard let index = list.firstIndex(where: { $0.accountHolderName == holder }) else {
            throw APIError(status: 404, body: "mock.delete_missing".formatted(holder))
        }
        list.remove(at: index)
        return "mock.deleted".formatted(holder)
    }
    func deposit(id: String, amount: Double) throws -> Account {
        guard !amount.isNaN, amount > 0 else { throw APIError(status: 400, body: "mock.deposit_invalid".localizedString) }
        guard let index = list.firstIndex(where: { $0.id == id }) else {
            throw APIError(status: 404, body: "mock.deposit_missing".formatted(id))
        }
        list[index].balance = floor((list[index].balance + amount) * 100 + 0.5) / 100
        return list[index]
    }
    func transaction(items: [TransactionItem]) -> String {
        (items.allSatisfy { available.contains($0.assetCode) } ? "mock.transaction_success" : "mock.transaction_fallback").localizedString
    }
    func assets(codes: [String]) -> [AssetAvailability] {
        codes.map { AssetAvailability(assetCode: $0, isAssetAvailable: available.contains($0)) }
    }
}
