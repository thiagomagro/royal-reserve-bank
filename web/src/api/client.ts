const TOKEN_KEY = "rrb.token";

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function getToken(): string | undefined {
  return localStorage.getItem(TOKEN_KEY) ?? (import.meta.env.VITE_API_TOKEN as string | undefined);
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const baseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";
  const headers = new Headers(init.headers);

  const token = getToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${baseUrl}${path}`, { ...init, headers });
  if (!response.ok) {
    const message = await response.text();
    throw new ApiError(response.status, message || response.statusText);
  }
  return response;
}

export async function apiFetchJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await apiFetch(path, init);
  return (await response.json()) as T;
}

export async function apiFetchText(path: string, init: RequestInit = {}): Promise<string> {
  const response = await apiFetch(path, init);
  return response.text();
}
