import Foundation
import SwiftUI

extension String {
    var localized: LocalizedStringKey { LocalizedStringKey(self) }
    var localizedString: String { NSLocalizedString(self, comment: "\(self)_comment") }
    func formatted(_ args: CVarArg...) -> String { String(format: localizedString, arguments: args) }
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
}

struct Account: Codable, Identifiable, Equatable {
    let id: String
    let accountNumber: String
    let accountHolderName: String
    var balance: Double
    let currency: String
}

struct AssetAvailability: Decodable, Equatable {
    let assetCode: String
    let isAssetAvailable: Bool
    init(assetCode: String, isAssetAvailable: Bool) {
        self.assetCode = assetCode
        self.isAssetAvailable = isAssetAvailable
    }
    enum CodingKeys: String, CodingKey { case assetCode, isAssetAvailable, assetAvailable }
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        assetCode = try values.decode(String.self, forKey: .assetCode)
        isAssetAvailable = try values.decodeIfPresent(Bool.self, forKey: .isAssetAvailable)
            ?? values.decodeIfPresent(Bool.self, forKey: .assetAvailable) ?? false
    }
}

struct TransactionItem: Codable, Equatable {
    let assetCode: String
    let assetName: String
    let value: Int
}

struct TransactionRow: Identifiable {
    let id = UUID()
    var code = ""
    var name = ""
    var value = "1"
    var available: Bool?
}

enum Validation {
    static func moneyInput(_ input: String, minimum: Double) -> Bool {
        guard let value = Double(input.trimmed), value.isFinite, value >= minimum else { return false }
        return abs(value * 100 - (value * 100).rounded()) < 0.000001
    }
    static func transactionForm(_ rows: [TransactionRow]) -> Bool {
        !rows.isEmpty && rows.allSatisfy {
            guard !$0.code.isEmpty, let number = Double($0.value.trimmed), number.isFinite else { return false }
            return number >= 1 && number.rounded() == number
        }
    }
    static func account(_ holder: String, _ balance: String) -> Double? {
        guard !holder.trimmed.isEmpty, !balance.trimmed.isEmpty,
              let value = Double(balance.trimmed), value.isFinite, value >= 0 else { return nil }
        return value
    }
    static func deposit(_ amount: String) -> Double? {
        guard let value = Double(amount.trimmed), value.isFinite, value > 0 else { return nil }
        return value
    }
    static func parseInt(_ input: String) -> Int? {
        let text = input.trimmed
        let sign = text.first == "-" ? -1 : 1
        let digits = (text.first == "-" || text.first == "+" ? String(text.dropFirst()) : text)
            .prefix { $0 >= "0" && $0 <= "9" }
        guard !digits.isEmpty, let value = Int(digits) else { return nil }
        return value * sign
    }
    static func transaction(_ rows: [TransactionRow]) -> [TransactionItem]? {
        let nonempty = rows.filter { !$0.code.trimmed.isEmpty }
        guard !nonempty.isEmpty else { return nil }
        var result: [TransactionItem] = []
        for row in nonempty {
            guard let value = parseInt(row.value), value > 0 else { return nil }
            result.append(TransactionItem(assetCode: row.code.trimmed, assetName: row.name.trimmed, value: value))
        }
        return result
    }
    static func codes(_ input: String) -> [String] {
        input.components(separatedBy: CharacterSet.whitespacesAndNewlines.union(CharacterSet(charactersIn: ",")))
            .filter { !$0.isEmpty }
    }
}

func formatCurrency(_ value: Double, _ currency: String) -> String {
    guard currency.count == 3, currency.unicodeScalars.allSatisfy({ CharacterSet.letters.contains($0) && $0.isASCII }) else {
        return String(format: "%@ %.2f", locale: Locale(identifier: "en_US"), currency, value)
    }
    let formatter = NumberFormatter()
    formatter.locale = Locale(identifier: "en_US")
    formatter.numberStyle = .currency
    formatter.currencyCode = currency.uppercased()
    return formatter.string(from: NSNumber(value: value)) ?? String(format: "%@ %.2f", currency, value)
}
