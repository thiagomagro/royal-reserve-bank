import { apiFetchJson, apiFetchText } from "./client";
import type { AccountResponse, CreateAccountRequest, DeleteAccountRequest } from "./types";

export function getAccounts(): Promise<AccountResponse[]> {
  return apiFetchJson<AccountResponse[]>("/api/account");
}

export function createAccount(body: CreateAccountRequest): Promise<string> {
  return apiFetchText("/api/account", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function deleteAccount(body: DeleteAccountRequest): Promise<string> {
  return apiFetchText("/api/account", {
    method: "DELETE",
    body: JSON.stringify(body),
  });
}
