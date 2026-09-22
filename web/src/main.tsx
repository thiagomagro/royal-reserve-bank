import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import App from "./App";
import { ToastProvider } from "./components/ToastContext";
import "./index.css";

async function enableMocking(): Promise<void> {
  if (import.meta.env.VITE_API_MOCK !== "true") {
    return;
  }
  const { startMockWorker } = await import("./mocks/browser");
  await startMockWorker();
}

const queryClient = new QueryClient();

enableMocking().then(() => {
  createRoot(document.getElementById("root")!).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </ToastProvider>
      </QueryClientProvider>
    </StrictMode>,
  );
});
