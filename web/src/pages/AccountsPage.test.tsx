import { describe, expect, it } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { AccountsPage } from "./AccountsPage";
import { ToastProvider } from "../components/ToastContext";

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter>
          <AccountsPage />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
}

describe("AccountsPage", () => {
  it("renders the seeded accounts", async () => {
    renderPage();
    expect(await screen.findByText("Alice Johnson")).toBeInTheDocument();
    expect(screen.getByText("Bruno Souza")).toBeInTheDocument();
    expect(screen.getByText("Carla Mendes")).toBeInTheDocument();
  });

  it("creating an account adds a row", async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Alice Johnson");

    await user.click(screen.getByRole("button", { name: "New account" }));
    await user.type(screen.getByPlaceholderText("Jane Doe"), "Diana Prince");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    await waitFor(() => expect(screen.getByText("Diana Prince")).toBeInTheDocument());
    expect(
      await screen.findByText("Successfully set up a new bank account for Diana Prince."),
    ).toBeInTheDocument();
  });
});
