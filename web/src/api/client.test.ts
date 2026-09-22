import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { apiFetch, ApiError } from "./client";
import { server } from "../mocks/server";

describe("apiFetch", () => {
  it("adds the bearer token from localStorage", async () => {
    localStorage.setItem("rrb.token", "test-token-123");
    let authHeader: string | null = null;
    server.use(
      http.get("/api/echo-auth", ({ request }) => {
        authHeader = request.headers.get("Authorization");
        return HttpResponse.text("ok");
      }),
    );

    await apiFetch("/api/echo-auth");
    expect(authHeader).toBe("Bearer test-token-123");
  });

  it("throws ApiError with the text body on 404", async () => {
    server.use(
      http.delete("/api/account", () =>
        HttpResponse.text("No bank account found for Nobody.", { status: 404 }),
      ),
    );

    await expect(
      apiFetch("/api/account", {
        method: "DELETE",
        body: JSON.stringify({ accountHolderName: "Nobody" }),
      }),
    ).rejects.toMatchObject({
      name: "ApiError",
      status: 404,
      message: "No bank account found for Nobody.",
    });
    await expect(
      apiFetch("/api/account", {
        method: "DELETE",
        body: JSON.stringify({ accountHolderName: "Nobody" }),
      }),
    ).rejects.toBeInstanceOf(ApiError);
  });
});
