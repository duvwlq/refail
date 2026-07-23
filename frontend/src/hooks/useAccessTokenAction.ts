"use client";

import { useRouter } from "next/navigation";
import { getAccessToken } from "@/lib/auth";

export function useAccessTokenAction() {
  const router = useRouter();

  return function requireAccessToken(): string | null {
    const token = getAccessToken();
    if (!token) router.push("/login");
    return token;
  };
}
