import { useState, type FormEvent } from "react";
import { clearAccessToken, getAccessToken, setAccessToken } from "../api/client";
import { useToast } from "../components/ToastContext";

export function SettingsPage() {
  const { showToast } = useToast();
  const [token, setToken] = useState(() => getAccessToken() ?? "");
  const mockEnabled = import.meta.env.VITE_API_MOCK === "true";

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    setAccessToken(token);
    showToast("success", "API token saved.");
  }

  function onClear() {
    clearAccessToken();
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

      <form
        onSubmit={onSubmit}
        className="w-full max-w-full rounded-lg bg-white p-6 shadow md:max-w-xl"
      >
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
          The token is kept in memory only for this browser tab and sent as a Bearer token on every
          API request. It is never written to localStorage or cookies, so you will need to paste it
          again after a page reload.
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
