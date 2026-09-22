import { test, expect } from "@playwright/test";

test.use({ viewport: { width: 390, height: 844 } });

const routes = ["/", "/accounts", "/transactions/new", "/assets", "/settings"];

for (const route of routes) {
  test(`no horizontal overflow at 390px on ${route}`, async ({ page }) => {
    await page.goto(route);
    await page.waitForLoadState("networkidle");
    const metrics = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      innerWidth: window.innerWidth,
    }));
    expect(metrics.scrollWidth).toBeLessThanOrEqual(metrics.innerWidth);
  });
}

test("unknown route shows the not found page", async ({ page }) => {
  await page.goto("/not-a-real-route");
  await expect(page.getByText("Page not found")).toBeVisible();
  await expect(page.getByRole("link", { name: "Back to Dashboard" })).toBeVisible();
});
