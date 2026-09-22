import { test, expect } from "@playwright/test";

test("create account appears in the table", async ({ page }) => {
  await page.goto("/accounts");
  await expect(page.getByText("Alice Johnson")).toBeVisible();

  await page.getByRole("button", { name: "New account" }).click();
  await page.getByPlaceholder("Jane Doe").fill("Eve Tester");
  await page.getByRole("button", { name: "Create account" }).click();

  await expect(page.getByRole("cell", { name: "Eve Tester" })).toBeVisible();
  await expect(
    page.getByText("Successfully set up a new bank account for Eve Tester."),
  ).toBeVisible();
});

test("new transaction with PETR4 shows success message", async ({ page }) => {
  await page.goto("/transactions/new");
  await page.getByPlaceholder("PETR4").fill("PETR4");
  await page.getByRole("button", { name: "Submit transaction" }).click();
  await expect(page.getByText("Transaction placed successfully.")).toBeVisible();
});

test("assets check shows availability badges", async ({ page }) => {
  await page.goto("/assets");
  await page.getByPlaceholder("PETR4, VALE3, AAPL").fill("PETR4, FOO42");
  await page.getByRole("button", { name: "Check availability" }).click();
  await expect(page.getByText("Available", { exact: true })).toBeVisible();
  await expect(page.getByText("Unavailable")).toBeVisible();
});

test("dashboard shows account totals and navigates to accounts", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
  await expect(page.getByText(/^Total balance \(/).first()).toBeVisible();
  await expect(page.getByRole("heading", { name: "Top accounts" })).toBeVisible();
  await expect(page.getByRole("cell", { name: "Alice Johnson" })).toBeVisible();

  await page.getByRole("link", { name: "Manage accounts" }).click();
  await expect(page).toHaveURL(/\/accounts$/);
  await expect(page.getByRole("heading", { name: "Accounts" })).toBeVisible();
});

test("settings saves and clears the API token", async ({ page }) => {
  await page.goto("/settings");
  await expect(page.getByRole("heading", { name: "Settings" })).toBeVisible();
  await expect(page.getByText("API mock mode:")).toContainText("enabled");

  await page.getByPlaceholder("eyJhbGciOi…").fill("test-token-123");
  await page.getByRole("button", { name: "Save token" }).click();
  await expect(page.getByText("API token saved.")).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem("rrb.token"))).toBe("test-token-123");

  await page.getByRole("button", { name: "Clear token" }).click();
  await expect(page.getByText("API token cleared.")).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem("rrb.token"))).toBeNull();
});
