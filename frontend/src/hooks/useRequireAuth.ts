"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getCurrentUser } from "@/lib/api/auth";
import { getAccessToken } from "@/lib/auth";
import type { AuthUser } from "@/types/auth";

type AuthGuardState = {
  ready: boolean;
  token: string | null;
  user: AuthUser | null;
};

export function useRequireAuth(requiredRole?: AuthUser["role"]): AuthGuardState {
  const router = useRouter();
  const [state, setState] = useState<AuthGuardState>({
    ready: false,
    token: null,
    user: null,
  });

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      router.replace("/login");
      return;
    }

    getCurrentUser(token)
      .then((user) => {
        if (requiredRole && user.role !== requiredRole) {
          router.replace("/");
          return;
        }
        setState({ ready: true, token, user });
      })
      .catch(() => router.replace("/login"));
  }, [requiredRole, router]);

  return state;
}
