import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { createTransaction } from "../api/transactions";
import { checkAssetsAvailability } from "../api/assets";
import { ApiError } from "../api/client";
import { useToast } from "../components/ToastContext";

interface ItemRow {
  assetCode: string;
  assetName: string;
  value: string;
  availability?: boolean;
}

const emptyRow = (): ItemRow => ({ assetCode: "", assetName: "", value: "1" });

export function NewTransactionPage() {
  const { showToast } = useToast();
  const [items, setItems] = useState<ItemRow[]>([emptyRow()]);

  const availabilityMutation = useMutation({
    mutationFn: checkAssetsAvailability,
    onSuccess: (results) => {
      const map = new Map(results.map((r) => [r.assetCode, r.isAssetAvailable]));
      setItems((prev) =>
        prev.map((item) => ({
          ...item,
          availability: item.assetCode ? map.get(item.assetCode) : undefined,
        })),
      );
    },
    onError: (err) => {
      showToast(
        "error",
        err instanceof ApiError ? err.message : "Failed to check asset availability",
      );
    },
  });

  const submitMutation = useMutation({
    mutationFn: createTransaction,
    onSuccess: (message) => {
      if (message.includes("Oops")) {
        showToast("error", message);
      } else {
        showToast("success", message);
      }
    },
    onError: (err) => {
      showToast("error", err instanceof ApiError ? err.message : "Transaction failed");
    },
  });

  function updateItem(index: number, patch: Partial<ItemRow>) {
    setItems((prev) =>
      prev.map((item, i) => (i === index ? { ...item, ...patch, availability: undefined } : item)),
    );
  }

  function onCheckAvailability() {
    const codes = items.map((i) => i.assetCode.trim()).filter(Boolean);
    if (codes.length === 0) {
      showToast("error", "Enter at least one asset code first.");
      return;
    }
    availabilityMutation.mutate(codes);
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    const parsed = items
      .filter((i) => i.assetCode.trim())
      .map((i) => ({
        assetCode: i.assetCode.trim(),
        assetName: i.assetName.trim(),
        value: Number.parseInt(i.value, 10),
      }));
    if (parsed.length === 0 || parsed.some((i) => !Number.isInteger(i.value) || i.value <= 0)) {
      showToast("error", "Each item needs an asset code and a positive integer value.");
      return;
    }
    submitMutation.mutate({ transactionItemsDtoList: parsed });
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-slate-900">New transaction</h1>

      <form onSubmit={onSubmit} className="rounded-lg bg-white p-6 shadow">
        <table className="mb-4 w-full text-left text-sm">
          <thead>
            <tr className="border-b text-slate-500">
              <th className="py-2 pr-3">Asset code</th>
              <th className="py-2 pr-3">Asset name</th>
              <th className="py-2 pr-3">Value</th>
              <th className="py-2 pr-3">Availability</th>
              <th className="py-2" />
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <tr key={index} className="border-b last:border-0">
                <td className="py-2 pr-3">
                  <input
                    required
                    value={item.assetCode}
                    onChange={(e) => updateItem(index, { assetCode: e.target.value })}
                    className="w-full rounded-md border border-slate-300 px-2 py-1"
                    placeholder="PETR4"
                  />
                </td>
                <td className="py-2 pr-3">
                  <input
                    value={item.assetName}
                    onChange={(e) => updateItem(index, { assetName: e.target.value })}
                    className="w-full rounded-md border border-slate-300 px-2 py-1"
                    placeholder="Petrobras PN"
                  />
                </td>
                <td className="py-2 pr-3">
                  <input
                    required
                    type="number"
                    min="1"
                    step="1"
                    value={item.value}
                    onChange={(e) => updateItem(index, { value: e.target.value })}
                    className="w-24 rounded-md border border-slate-300 px-2 py-1"
                  />
                </td>
                <td className="py-2 pr-3">
                  {item.availability === true && (
                    <span className="rounded-full bg-emerald-100 px-2 py-1 text-xs font-medium text-emerald-700">
                      Available
                    </span>
                  )}
                  {item.availability === false && (
                    <span className="rounded-full bg-red-100 px-2 py-1 text-xs font-medium text-red-700">
                      Unavailable
                    </span>
                  )}
                </td>
                <td className="py-2 text-right">
                  <button
                    type="button"
                    onClick={() => setItems((prev) => prev.filter((_, i) => i !== index))}
                    disabled={items.length === 1}
                    className="rounded-md border border-slate-300 px-2 py-1 text-xs text-slate-600 hover:bg-slate-100 disabled:opacity-40"
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setItems((prev) => [...prev, emptyRow()])}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            Add item
          </button>
          <button
            type="button"
            onClick={onCheckAvailability}
            disabled={availabilityMutation.isPending}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50"
          >
            {availabilityMutation.isPending ? "Checking…" : "Check availability"}
          </button>
          <button
            type="submit"
            disabled={submitMutation.isPending}
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
          >
            {submitMutation.isPending ? "Submitting…" : "Submit transaction"}
          </button>
        </div>
      </form>
    </div>
  );
}
