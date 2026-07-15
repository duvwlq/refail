import Link from "next/link";
import { notFound } from "next/navigation";
import { PostForm } from "@/components/post/PostForm";
import { apiFetch } from "@/lib/api";
import type { PostDetail } from "@/types/post";
import styles from "./page.module.css";

async function getPost(postId: string): Promise<PostDetail | null> {
  try {
    return await apiFetch<PostDetail>(`/api/v1/posts/${postId}`, { cache: "no-store" });
  } catch {
    return null;
  }
}

export default async function EditPostPage({ params }: { params: Promise<{ postId: string }> }) {
  const { postId } = await params;
  if (!/^\d+$/.test(postId)) notFound();
  const post = await getPost(postId);
  if (!post) notFound();

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <Link href="/" className={styles.brand}>Re:<span>Fail</span></Link>
        <Link href={`/posts/${postId}`} className={styles.close}>수정 취소</Link>
      </header>
      <section className={styles.intro}>
        <span>기록 수정</span>
        <h1>실패를 다시 돌아봅니다.</h1>
        <p>그때 미처 적지 못했던 맥락이나 다음 선택을 더 정확하게 다듬어 보세요.</p>
      </section>
      <PostForm initialPost={post} />
    </main>
  );
}
