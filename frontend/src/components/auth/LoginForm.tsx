"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api";
import { login } from "@/lib/api/auth";
import { setAuth } from "@/lib/auth";
import styles from "./AuthForm.module.css";

type LoginFormProps = {
  initialEmail?: string;
  joined?: boolean;
};

export function LoginForm({ initialEmail = "", joined = false }: LoginFormProps) {
  const router = useRouter();
  const [email, setEmail] = useState(initialEmail);
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setPending(true);
    try {
      const response = await login(email, password);
      setAuth(response.accessToken, response.user);
      router.push("/");
      router.refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "로그인 중 문제가 발생했습니다.");
    } finally {
      setPending(false);
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.formHeader}>
        <span>WELCOME BACK</span>
        <h2>기록을 이어가세요.</h2>
      </div>
      {joined && <p className={styles.notice}>가입이 완료되었습니다. 이제 로그인해 주세요.</p>}
      <div className={styles.fields}>
        <div className={styles.field}>
          <label htmlFor="email">이메일</label>
          <input id="email" name="email" type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="name@example.com" />
        </div>
        <div className={styles.field}>
          <label htmlFor="password">비밀번호</label>
          <input id="password" name="password" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} placeholder="비밀번호를 입력하세요" />
        </div>
      </div>
      {error && <p className={styles.error} role="alert">{error}</p>}
      <button className={styles.submit} type="submit" disabled={pending}>{pending ? "확인하는 중..." : "로그인"}</button>
      <p className={styles.switch}>아직 계정이 없나요?<Link href="/signup">회원가입</Link></p>
    </form>
  );
}
