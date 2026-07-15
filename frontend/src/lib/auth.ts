const ACCESS_TOKEN_KEY = "fail.accessToken";
const AUTH_USER_KEY = "fail.user";
export const AUTH_CHANGED_EVENT = "fail:auth-changed";

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setAuth(token: string, user: object): void {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, token);
  window.localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}

export function clearAuth(): void {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(AUTH_USER_KEY);
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}
