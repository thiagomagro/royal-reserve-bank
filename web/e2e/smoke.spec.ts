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
