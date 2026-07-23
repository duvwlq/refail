"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api";
import { signup } from "@/lib/api/auth";
import styles from "./AuthForm.module.css";

export function SignupForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    if (password !== passwordConfirm) {
      setError("비밀번호가 서로 다릅니다.");
      return;
    }
    setPending(true);
    try {
      await signup(email, password, nickname);
      router.push(`/login?joined=true&email=${encodeURIComponent(email)}`);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "회원가입 중 문제가 발생했습니다.");
    } finally {
      setPending(false);
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.formHeader}>
        <span>NEW ARCHIVIST</span>
        <h2>나만의 서랍 만들기</h2>
      </div>
      <div className={styles.fields}>
        <div className={styles.field}>
          <label htmlFor="email">이메일</label>
          <input id="email" type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="name@example.com" />
        </div>
        <div className={styles.field}>
          <label htmlFor="nickname">닉네임</label>
          <input id="nickname" type="text" autoComplete="nickname" minLength={2} maxLength={30} required value={nickname} onChange={(event) => setNickname(event.target.value)} placeholder="2~30자" />
          <small className={styles.hint}>게시글마다 익명 또는 닉네임 공개를 선택할 수 있습니다.</small>
        </div>
        <div className={styles.field}>
          <label htmlFor="password">비밀번호</label>
          <input id="password" type="password" autoComplete="new-password" minLength={8} required value={password} onChange={(event) => setPassword(event.target.value)} placeholder="8자 이상" />
        </div>
        <div className={styles.field}>
          <label htmlFor="password-confirm">비밀번호 확인</label>
          <input id="password-confirm" type="password" autoComplete="new-password" minLength={8} required value={passwordConfirm} onChange={(event) => setPasswordConfirm(event.target.value)} placeholder="한 번 더 입력하세요" />
        </div>
      </div>
      {error && <p className={styles.error} role="alert">{error}</p>}
      <button className={styles.submit} type="submit" disabled={pending}>{pending ? "서랍 만드는 중..." : "회원가입"}</button>
      <p className={styles.switch}>이미 계정이 있나요?<Link href="/login">로그인</Link></p>
    </form>
  );
}
