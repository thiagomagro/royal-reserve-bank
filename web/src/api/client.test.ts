import { afterEach, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { apiFetch, ApiError, clearAccessToken, setAccessToken } from "./client";
import { server } from "../mocks/server";

afterEach(() => {
  clearAccessToken();
});

describe("apiFetch", () => {
  it("adds the bearer token from the in-memory store", async () => {
    setAccessToken("test-token-123");
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

  it("does not send Authorization when no token is set", async () => {
    let authHeader: string | null = "unset";
    server.use(
      http.get("/api/echo-auth", ({ request }) => {
        authHeader = request.headers.get("Authorization");
        return HttpResponse.text("ok");
      }),
    );

    await apiFetch("/api/echo-auth");
    expect(authHeader).toBeNull();
  });

  it("purges a legacy rrb.token from localStorage on load", async () => {
    localStorage.setItem("rrb.token", "x");
    vi.resetModules();
    await import("./client");
    expect(localStorage.getItem("rrb.token")).toBeNull();
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
