"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getCurrentUser, logout as requestLogout } from "@/lib/api/auth";
import { AUTH_CHANGED_EVENT, clearAuth, getAccessToken } from "@/lib/auth";
import type { AuthUser } from "@/types/auth";
import styles from "./AuthNav.module.css";

export function AuthNav() {
  const [user, setUser] = useState<AuthUser | null>(null);

  async function logout() {
    try {
      await requestLogout();
    } finally {
      clearAuth();
      setUser(null);
    }
  }

  useEffect(() => {
    async function loadUser() {
      const token = getAccessToken();
      if (!token) {
        setUser(null);
        return;
      }
      try {
        setUser(await getCurrentUser(token));
      } catch {
        clearAuth();
        setUser(null);
      }
    }

    void loadUser();
    window.addEventListener(AUTH_CHANGED_EVENT, loadUser);
    window.addEventListener("storage", loadUser);
    return () => {
      window.removeEventListener(AUTH_CHANGED_EVENT, loadUser);
      window.removeEventListener("storage", loadUser);
    };
  }, []);

  return (
    <nav className={styles.nav} aria-label="주요 메뉴">
      <Link href={user ? "/my-records" : "/#records"} className={styles.records}>{user ? "내 기록" : "실패 기록"}</Link>
      {user?.role === "ADMIN" && <Link href="/admin" className={styles.records}>운영</Link>}
      {user ? (
        <>
          <span className={styles.user}>{user.nickname}</span>
          <button type="button" onClick={() => void logout()}>로그아웃</button>
        </>
      ) : (
        <Link href="/login">로그인</Link>
      )}
      <Link href={user ? "/posts/new" : "/login"} className={styles.writeButton}>
        기록 남기기
      </Link>
    </nav>
  );
}
