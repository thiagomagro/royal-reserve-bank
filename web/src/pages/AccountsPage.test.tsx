import { describe, expect, it } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
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

  it("deleting an account removes the row", async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Alice Johnson");

    const row = screen.getByText("Alice Johnson").closest("tr")!;
    await user.click(within(row).getByRole("button", { name: "Delete" }));

    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Delete" }));

    await waitFor(() => expect(screen.queryByText("Alice Johnson")).not.toBeInTheDocument());
    expect(
      await screen.findByText("Successfully deleted the bank account for Alice Johnson."),
    ).toBeInTheDocument();
  });
});
