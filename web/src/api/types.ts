export type Currency = "USD" | "EUR" | "GBP" | "BRL";

export interface AccountResponse {
  id: string;
  accountNumber: string;
  accountHolderName: string;
  balance: number;
  currency: string;
}

export interface CreateAccountRequest {
  accountHolderName: string;
  balance: number;
  currency: Currency;
}

export interface DeleteAccountRequest {
  accountHolderName: string;
}

export interface TransactionItem {
  assetCode: string;
  assetName: string;
  value: number;
}

export interface TransactionRequest {
  transactionItemsDtoList: TransactionItem[];
}

export interface AssetAvailability {
  assetCode: string;
  isAssetAvailable: boolean;
}
