import { http, HttpResponse } from "msw";
import type { CreateAccountRequest, DeleteAccountRequest, TransactionRequest } from "../api/types";
import { AVAILABLE_ASSETS, accounts, addAccount, removeAccount } from "./store";

export const TRANSACTION_FALLBACK_MESSAGE = "Oops! Something went wrong, please try again later!";

export const handlers = [
  http.post("/api/account", async ({ request }) => {
    const body = (await request.json()) as CreateAccountRequest;
    addAccount(body.accountHolderName, body.balance, body.currency);
    return HttpResponse.text(
      `Successfully set up a new bank account for ${body.accountHolderName}.`,
      { status: 201 },
    );
  }),

  http.get("/api/account", () => {
    return HttpResponse.json(accounts);
  }),

  http.delete("/api/account", async ({ request }) => {
    const body = (await request.json()) as DeleteAccountRequest;
    const removed = removeAccount(body.accountHolderName);
    if (!removed) {
      return HttpResponse.text(`No bank account found for ${body.accountHolderName}.`, {
        status: 404,
      });
    }
    return HttpResponse.text(
      `Successfully deleted the bank account for ${body.accountHolderName}.`,
    );
  }),

  http.post("/api/transaction", async ({ request }) => {
    const body = (await request.json()) as TransactionRequest;
    const allAvailable = body.transactionItemsDtoList.every((item) =>
      AVAILABLE_ASSETS.has(item.assetCode),
    );
    if (!allAvailable) {
      return HttpResponse.text(TRANSACTION_FALLBACK_MESSAGE, { status: 201 });
    }
    return HttpResponse.text("Transaction placed successfully.", { status: 201 });
  }),

  http.get("/api/asset-management", ({ request }) => {
    const url = new URL(request.url);
    const codes = url.searchParams.getAll("assetCode");
    return HttpResponse.json(
      codes.map((assetCode) => ({
        assetCode,
        isAssetAvailable: AVAILABLE_ASSETS.has(assetCode),
      })),
    );
  }),
];
