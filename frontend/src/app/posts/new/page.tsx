import Link from "next/link";
import { PostForm } from "@/components/post/PostForm";
import styles from "./page.module.css";

export default function NewPostPage() {
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <Link href="/" className={styles.brand}>Re:<span>Fail</span></Link>
        <Link href="/" className={styles.close}>나가기</Link>
      </header>
      <section className={styles.intro}>
        <span>새 실패 기록</span>
        <h1>오늘의 실패를 기록해 보세요.</h1>
        <p>잘 쓰려고 애쓰지 않아도 괜찮아요. 있었던 일을 있는 그대로 남겨 주세요.</p>
      </section>
      <PostForm />
    </main>
  );
}
