const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:18080";

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
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...requestInit,
    headers: {
      "Content-Type": "application/json",
      ...headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

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
