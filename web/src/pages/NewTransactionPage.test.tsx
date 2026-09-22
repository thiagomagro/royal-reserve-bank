import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { NewTransactionPage } from "./NewTransactionPage";
import { ToastProvider } from "../components/ToastContext";

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter>
          <NewTransactionPage />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
}

describe("NewTransactionPage", () => {
  it("shows availability badges after checking", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText("PETR4"), "PETR4");
    await user.click(screen.getByRole("button", { name: "Check availability" }));

    expect(await screen.findByText("Available")).toBeInTheDocument();
  });

  it("submit shows the success message", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText("PETR4"), "PETR4");
    await user.click(screen.getByRole("button", { name: "Submit transaction" }));

    expect(await screen.findByText("Transaction placed successfully.")).toBeInTheDocument();
  });
});
