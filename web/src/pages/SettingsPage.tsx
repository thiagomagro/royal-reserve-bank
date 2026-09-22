import { useState, type FormEvent } from "react";
import { useToast } from "../components/ToastContext";

const TOKEN_KEY = "rrb.token";

export function SettingsPage() {
  const { showToast } = useToast();
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? "");
  const mockEnabled = import.meta.env.VITE_API_MOCK === "true";

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    localStorage.setItem(TOKEN_KEY, token.trim());
    showToast("success", "API token saved.");
  }

  function onClear() {
    localStorage.removeItem(TOKEN_KEY);
    setToken("");
    showToast("info", "API token cleared.");
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Settings</h1>

      <div className="mb-6 rounded-lg bg-white p-6 shadow">
        <p className="text-sm text-slate-700">
          API mock mode:{" "}
          <span className="font-semibold">{mockEnabled ? "enabled" : "disabled"}</span>
        </p>
      </div>

      <form onSubmit={onSubmit} className="max-w-xl rounded-lg bg-white p-6 shadow">
        <label className="mb-4 flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-700">API token (Auth0 JWT)</span>
          <textarea
            value={token}
            onChange={(e) => setToken(e.target.value)}
            rows={4}
            className="rounded-md border border-slate-300 px-3 py-2 font-mono text-xs"
            placeholder="eyJhbGciOi…"
          />
        </label>
        <p className="mb-4 text-xs text-slate-500">
          The token is stored in your browser&apos;s local storage under <code>{TOKEN_KEY}</code>{" "}
          and sent as a Bearer token on every API request.
        </p>
        <div className="flex gap-2">
          <button
            type="submit"
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
          >
            Save token
          </button>
          <button
            type="button"
            onClick={onClear}
            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            Clear token
          </button>
        </div>
      </form>
    </div>
  );
}
