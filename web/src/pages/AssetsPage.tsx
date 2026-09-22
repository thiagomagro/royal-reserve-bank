import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { checkAssetsAvailability } from "../api/assets";
import type { AssetAvailability } from "../api/types";
import { ApiError } from "../api/client";
import { useToast } from "../components/ToastContext";

export function AssetsPage() {
  const { showToast } = useToast();
  const [input, setInput] = useState("");
  const [results, setResults] = useState<AssetAvailability[]>([]);

  const mutation = useMutation({
    mutationFn: checkAssetsAvailability,
    onSuccess: setResults,
    onError: (err) => {
      showToast(
        "error",
        err instanceof ApiError ? err.message : "Failed to check asset availability",
      );
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    const codes = input
      .split(/[\s,]+/)
      .map((c) => c.trim())
      .filter(Boolean);
    if (codes.length === 0) {
      showToast("error", "Enter at least one asset code.");
      return;
    }
    mutation.mutate(codes);
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Assets</h1>

      <form onSubmit={onSubmit} className="mb-6 flex flex-wrap gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          className="w-full max-w-md rounded-md border border-slate-300 px-3 py-2 text-sm"
          placeholder="PETR4, VALE3, AAPL"
        />
        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
        >
          {mutation.isPending ? "Checking…" : "Check availability"}
        </button>
      </form>

      {results.length > 0 && (
        <div className="max-w-xl overflow-x-auto rounded-lg bg-white shadow">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b text-slate-500">
                <th className="px-4 py-3">Asset code</th>
                <th className="px-4 py-3">Status</th>
              </tr>
            </thead>
            <tbody>
              {results.map((asset) => (
                <tr key={asset.assetCode} className="border-b last:border-0">
                  <td className="px-4 py-3 font-medium">{asset.assetCode}</td>
                  <td className="px-4 py-3">
                    {asset.isAssetAvailable ? (
                      <span className="rounded-full bg-emerald-100 px-2 py-1 text-xs font-medium text-emerald-700">
                        Available
                      </span>
                    ) : (
                      <span className="rounded-full bg-red-100 px-2 py-1 text-xs font-medium text-red-700">
                        Unavailable
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
