import { NavLink, Outlet } from "react-router-dom";

const navItems = [
  { to: "/", label: "Dashboard", end: true },
  { to: "/accounts", label: "Accounts" },
  { to: "/transactions/new", label: "New Transaction" },
  { to: "/assets", label: "Assets" },
  { to: "/settings", label: "Settings" },
];

export function AppLayout() {
  return (
    <div className="flex min-h-screen bg-slate-100">
      <aside className="flex w-64 flex-col bg-slate-900 text-slate-100">
        <div className="flex items-center gap-3 px-5 py-6">
          <img src="/logo.png" alt="Royal Reserve Bank" className="h-10 w-10 rounded" />
          <div>
            <p className="text-sm font-semibold leading-tight">Royal Reserve</p>
            <p className="text-xs text-slate-400">Bank</p>
          </div>
        </div>
        <nav className="flex-1 px-3 py-2">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `mb-1 block rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? "bg-slate-700 text-white"
                    : "text-slate-300 hover:bg-slate-800 hover:text-white"
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <p className="px-5 py-4 text-xs text-slate-500">Royal Reserve Bank</p>
      </aside>
      <main className="flex-1 p-8">
        <Outlet />
      </main>
    </div>
  );
}
