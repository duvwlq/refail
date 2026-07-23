import { clearAuth, setAuth } from "@/lib/auth";
import type { LoginResponse } from "@/types/auth";

const PUBLIC_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:18080";
let refreshPromise: Promise<string> | null = null;

function getApiBaseUrl() {
  if (typeof window === "undefined") {
    return process.env.API_INTERNAL_BASE_URL ?? PUBLIC_API_BASE_URL;
  }
  return PUBLIC_API_BASE_URL;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

type ApiRequestInit = RequestInit & {
  token?: string | null;
};

export async function apiFetch<T>(path: string, init: ApiRequestInit = {}): Promise<T> {
  const { token, headers, ...requestInit } = init;
  let response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...requestInit,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (response.status === 401 && token && canRefresh(path) && typeof window !== "undefined") {
    try {
      const refreshedToken = await refreshAccessToken();
      response = await fetch(`${getApiBaseUrl()}${path}`, {
        ...requestInit,
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          ...headers,
          Authorization: `Bearer ${refreshedToken}`,
        },
      });
    } catch {
      clearAuth();
    }
  }

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      error?.code ?? "UNKNOWN_ERROR",
      error?.message ?? "요청을 처리하지 못했습니다.",
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

function canRefresh(path: string): boolean {
  return ![
    "/api/v1/auth/login",
    "/api/v1/auth/signup",
    "/api/v1/auth/refresh",
    "/api/v1/auth/logout",
  ].includes(path);
}

function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = fetch(`${getApiBaseUrl()}/api/v1/auth/refresh`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new ApiError(response.status, "AUTH_REFRESH_FAILED", "로그인이 만료되었습니다.");
        }
        const session = (await response.json()) as LoginResponse;
        setAuth(session.accessToken, session.user);
        return session.accessToken;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}
