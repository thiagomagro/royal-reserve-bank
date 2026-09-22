import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getAccounts } from "../api/accounts";
import { formatCurrency } from "../lib/format";

export function DashboardPage() {
  const {
    data: accounts,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["accounts"],
    queryFn: getAccounts,
  });

  if (isLoading) {
    return <p className="text-slate-500">Loading dashboard…</p>;
  }
  if (isError) {
    return (
      <p className="text-red-600">
        Failed to load accounts: {error instanceof Error ? error.message : "unknown error"}
      </p>
    );
  }

  const list = accounts ?? [];
  const totals = new Map<string, number>();
  for (const account of list) {
    totals.set(account.currency, (totals.get(account.currency) ?? 0) + account.balance);
  }
  const top = [...list].sort((a, b) => b.balance - a.balance).slice(0, 5);

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Dashboard</h1>

      <div className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-lg bg-white p-5 shadow">
          <p className="text-sm text-slate-500">Accounts</p>
          <p className="mt-1 text-3xl font-bold text-slate-900">{list.length}</p>
        </div>
        {[...totals.entries()].map(([currency, total]) => (
          <div key={currency} className="rounded-lg bg-white p-5 shadow">
            <p className="text-sm text-slate-500">Total balance ({currency})</p>
            <p className="mt-1 text-3xl font-bold text-slate-900">
              {formatCurrency(total, currency)}
            </p>
          </div>
        ))}
      </div>

      <div className="mb-8 overflow-x-auto rounded-lg bg-white p-5 shadow">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">Top accounts</h2>
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b text-slate-500">
              <th className="py-2">Holder</th>
              <th className="py-2">Account #</th>
              <th className="py-2 text-right">Balance</th>
            </tr>
          </thead>
          <tbody>
            {top.map((account) => (
              <tr key={account.id} className="border-b last:border-0">
                <td className="py-2">{account.accountHolderName}</td>
                <td className="py-2 text-slate-500">{account.accountNumber}</td>
                <td className="py-2 text-right">
                  {formatCurrency(account.balance, account.currency)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex gap-3">
        <Link
          to="/accounts"
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
        >
          Manage accounts
        </Link>
        <Link
          to="/transactions/new"
          className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-200"
        >
          New transaction
        </Link>
        <Link
          to="/assets"
          className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-200"
        >
          Check assets
        </Link>
      </div>
    </div>
  );
}
