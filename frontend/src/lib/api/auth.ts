import { apiFetch } from "@/lib/api";
import type { AuthUser, LoginResponse } from "@/types/auth";

export function login(email: string, password: string): Promise<LoginResponse> {
  return apiFetch<LoginResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function signup(email: string, password: string, nickname: string): Promise<AuthUser> {
  return apiFetch<AuthUser>("/api/v1/auth/signup", {
    method: "POST",
    body: JSON.stringify({ email, password, nickname }),
  });
}

export function getCurrentUser(token: string): Promise<AuthUser> {
  return apiFetch<AuthUser>("/api/v1/auth/me", { token });
}

export function logout(): Promise<void> {
  return apiFetch("/api/v1/auth/logout", { method: "POST" });
}
