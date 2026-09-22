import type { AccountResponse } from "../api/types";

export const AVAILABLE_ASSETS = new Set(["PETR4", "VALE3", "AAPL", "BOND-BR-2030"]);

let nextId = 4;
let nextAccountNumber = 1004;

export const accounts: AccountResponse[] = [
  {
    id: "1",
    accountNumber: "1001",
    accountHolderName: "Alice Johnson",
    balance: 12500.5,
    currency: "USD",
  },
  {
    id: "2",
    accountNumber: "1002",
    accountHolderName: "Bruno Souza",
    balance: 8400,
    currency: "BRL",
  },
  {
    id: "3",
    accountNumber: "1003",
    accountHolderName: "Carla Mendes",
    balance: 3200,
    currency: "EUR",
  },
];

export function addAccount(holder: string, balance: number, currency: string): AccountResponse {
  const account: AccountResponse = {
    id: String(nextId++),
    accountNumber: String(nextAccountNumber++),
    accountHolderName: holder,
    balance,
    currency,
  };
  accounts.push(account);
  return account;
}

export function removeAccount(holder: string): AccountResponse | undefined {
  const index = accounts.findIndex((a) => a.accountHolderName === holder);
  if (index === -1) return undefined;
  return accounts.splice(index, 1)[0];
}

export function resetStore(): void {
  accounts.splice(
    0,
    accounts.length,
    {
      id: "1",
      accountNumber: "1001",
      accountHolderName: "Alice Johnson",
      balance: 12500.5,
      currency: "USD",
    },
    {
      id: "2",
      accountNumber: "1002",
      accountHolderName: "Bruno Souza",
      balance: 8400,
      currency: "BRL",
    },
    {
      id: "3",
      accountNumber: "1003",
      accountHolderName: "Carla Mendes",
      balance: 3200,
      currency: "EUR",
    },
  );
  nextId = 4;
  nextAccountNumber = 1004;
}
