import "@testing-library/jest-dom/vitest";
import { afterAll, afterEach, beforeAll, beforeEach } from "vitest";
import { server } from "../mocks/server";
import { resetStore } from "../mocks/store";

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
beforeEach(() => {
  resetStore();
  localStorage.clear();
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
