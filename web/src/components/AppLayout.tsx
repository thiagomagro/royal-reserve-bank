import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";

const navItems = [
  { to: "/", label: "Dashboard", end: true },
  { to: "/accounts", label: "Accounts" },
  { to: "/transactions/new", label: "New Transaction" },
  { to: "/assets", label: "Assets" },
  { to: "/settings", label: "Settings" },
];

function navLinkClass({ isActive }: { isActive: boolean }) {
  return `mb-1 block rounded-md px-3 py-2 text-sm font-medium transition-colors ${
    isActive ? "bg-slate-700 text-white" : "text-slate-300 hover:bg-slate-800 hover:text-white"
  }`;
}

export function AppLayout() {
  const [navOpen, setNavOpen] = useState(false);

  return (
    <div className="flex min-h-screen flex-col bg-slate-100 md:flex-row">
      {/* Mobile top bar */}
      <header className="flex items-center justify-between bg-slate-900 px-4 py-3 text-slate-100 md:hidden">
        <div className="flex items-center gap-3">
          <img src="/logo.png" alt="Royal Reserve Bank" className="h-8 w-8 rounded" />
          <span className="text-sm font-semibold">Royal Reserve Bank</span>
        </div>
        <button
          type="button"
          aria-label="Toggle navigation"
          aria-expanded={navOpen}
          onClick={() => setNavOpen((v) => !v)}
          className="rounded-md p-2 hover:bg-slate-800"
        >
          <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M4 6h16M4 12h16M4 18h16"
            />
          </svg>
        </button>
      </header>

      {/* Mobile drawer */}
      {navOpen && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={() => setNavOpen(false)} />
          <nav className="absolute left-0 top-0 h-full w-64 bg-slate-900 px-3 py-4">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                onClick={() => setNavOpen(false)}
                className={navLinkClass}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
      )}

      {/* Desktop sidebar */}
      <aside className="hidden w-64 flex-col bg-slate-900 text-slate-100 md:flex">
        <div className="flex items-center gap-3 px-5 py-6">
          <img src="/logo.png" alt="Royal Reserve Bank" className="h-10 w-10 rounded" />
          <div>
            <p className="text-sm font-semibold leading-tight">Royal Reserve</p>
            <p className="text-xs text-slate-400">Bank</p>
          </div>
        </div>
        <nav className="flex-1 px-3 py-2">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={navLinkClass}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <p className="px-5 py-4 text-xs text-slate-500">Royal Reserve Bank</p>
      </aside>

      <main className="min-w-0 flex-1 p-4 md:p-8">
        <Outlet />
      </main>
    </div>
  );
}
