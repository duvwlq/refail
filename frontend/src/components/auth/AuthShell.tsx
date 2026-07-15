import Link from "next/link";
import type { ReactNode } from "react";
import styles from "./AuthShell.module.css";

type AuthShellProps = {
  index: string;
  title: ReactNode;
  description: string;
  children: ReactNode;
};

export function AuthShell({ index, title, description, children }: AuthShellProps) {
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <Link href="/" className={styles.brand}>Re:<span>Fail</span></Link>
        <Link href="/" className={styles.back}>← 기록으로 돌아가기</Link>
      </header>
      <div className={styles.layout}>
        <section className={styles.intro}>
          <span className={styles.index}>{index}</span>
          <h1>{title}</h1>
          <p>{description}</p>
          <blockquote>
            실패를 숨기지 않는 순간,<br />다음 방법이 보이기 시작합니다.
          </blockquote>
        </section>
        <section className={styles.formArea}>{children}</section>
      </div>
    </main>
  );
}
