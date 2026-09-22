import { apiFetchText } from "./client";
import type { TransactionRequest } from "./types";

export function createTransaction(body: TransactionRequest): Promise<string> {
  return apiFetchText("/api/transaction", {
    method: "POST",
    body: JSON.stringify(body),
  });
}
