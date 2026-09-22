import { setupWorker } from "msw/browser";
import { handlers } from "./handlers";

export const worker = setupWorker(...handlers);

const workerOptions = {
  onUnhandledRequest: "bypass",
  serviceWorker: { url: "/mockServiceWorker.js" },
} as const;

export async function startMockWorker(): Promise<void> {
  await worker.start(workerOptions);
  navigator.serviceWorker?.addEventListener("controllerchange", () => {
    void worker.start(workerOptions);
  });
}
