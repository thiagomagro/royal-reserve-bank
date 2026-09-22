import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createAccount, deleteAccount, getAccounts } from "../api/accounts";
import { ApiError } from "../api/client";
import type { Currency } from "../api/types";
import { formatCurrency } from "../lib/format";
import { useToast } from "../components/ToastContext";
import { ConfirmDialog } from "../components/ConfirmDialog";

const CURRENCIES: Currency[] = ["USD", "EUR", "GBP", "BRL"];

export function AccountsPage() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);
  const [holderName, setHolderName] = useState("");
  const [balance, setBalance] = useState("0");
  const [currency, setCurrency] = useState<Currency>("USD");

  const {
    data: accounts,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["accounts"],
    queryFn: getAccounts,
  });

  const createMutation = useMutation({
    mutationFn: createAccount,
    onSuccess: (message) => {
      showToast("success", message);
      setDialogOpen(false);
      setHolderName("");
      setBalance("0");
      setCurrency("USD");
      void queryClient.invalidateQueries({ queryKey: ["accounts"] });
    },
    onError: (err) => {
      showToast("error", err instanceof ApiError ? err.message : "Failed to create account");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAccount,
    onSuccess: (message) => {
      showToast("success", message);
      void queryClient.invalidateQueries({ queryKey: ["accounts"] });
    },
    onError: (err) => {
      showToast("error", err instanceof ApiError ? err.message : "Failed to delete account");
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    const parsedBalance = Number(balance);
    if (!holderName.trim() || Number.isNaN(parsedBalance) || parsedBalance < 0) {
      showToast("error", "Provide a holder name and a balance of 0 or more.");
      return;
    }
    createMutation.mutate({
      accountHolderName: holderName.trim(),
      balance: parsedBalance,
      currency,
    });
  }

  function onDelete(accountHolderName: string) {
    setPendingDelete(accountHolderName);
  }

  function onConfirmDelete() {
    if (pendingDelete) {
      deleteMutation.mutate({ accountHolderName: pendingDelete });
    }
    setPendingDelete(null);
  }

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-slate-900">Accounts</h1>
        <button
          type="button"
          onClick={() => setDialogOpen(true)}
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
        >
          New account
        </button>
      </div>

      {isLoading && <p className="text-slate-500">Loading accounts…</p>}
      {isError && (
        <p className="text-red-600">
          Failed to load accounts: {error instanceof Error ? error.message : "unknown error"}
        </p>
      )}

      {accounts && (
        <div className="overflow-x-auto rounded-lg bg-white shadow">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b text-slate-500">
                <th className="px-4 py-3">Holder</th>
                <th className="px-4 py-3">Account #</th>
                <th className="px-4 py-3 text-right">Balance</th>
                <th className="px-4 py-3">Currency</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr key={account.id} className="border-b last:border-0">
                  <td className="px-4 py-3">{account.accountHolderName}</td>
                  <td className="px-4 py-3 text-slate-500">{account.accountNumber}</td>
                  <td className="px-4 py-3 text-right">
                    {formatCurrency(account.balance, account.currency)}
                  </td>
                  <td className="px-4 py-3">{account.currency}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      type="button"
                      onClick={() => onDelete(account.accountHolderName)}
                      className="rounded-md border border-red-300 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {dialogOpen && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h2 className="mb-4 text-lg font-semibold text-slate-900">New account</h2>
            <form onSubmit={onSubmit} className="flex flex-col gap-4">
              <label className="flex flex-col gap-1 text-sm">
                <span className="font-medium text-slate-700">Account holder name</span>
                <input
                  required
                  value={holderName}
                  onChange={(e) => setHolderName(e.target.value)}
                  className="rounded-md border border-slate-300 px-3 py-2"
                  placeholder="Jane Doe"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm">
                <span className="font-medium text-slate-700">Initial balance</span>
                <input
                  required
                  type="number"
                  min="0"
                  step="0.01"
                  value={balance}
                  onChange={(e) => setBalance(e.target.value)}
                  className="rounded-md border border-slate-300 px-3 py-2"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm">
                <span className="font-medium text-slate-700">Currency</span>
                <select
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value as Currency)}
                  className="rounded-md border border-slate-300 px-3 py-2"
                >
                  {CURRENCIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </label>
              <div className="mt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setDialogOpen(false)}
                  className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
                >
                  {createMutation.isPending ? "Creating…" : "Create account"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete account"
        message={pendingDelete ? `Delete the account for ${pendingDelete}?` : ""}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        onConfirm={onConfirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}
