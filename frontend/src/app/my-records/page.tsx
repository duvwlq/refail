"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { getAccessToken } from "@/lib/auth";
import type { PageResponse, PostSummary } from "@/types/post";
import styles from "./page.module.css";

export default function MyRecordsPage() {
  const router = useRouter();
  const [posts, setPosts] = useState<PostSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getAccessToken();
    if (!token) { router.replace("/login"); return; }
    apiFetch<PageResponse<PostSummary>>("/api/v1/posts/me?page=0&size=100", { token })
      .then((page) => setPosts(page.content))
      .finally(() => setLoading(false));
  }, [router]);

  return <main className={styles.page}>
    <header><Link href="/" className={styles.brand}>Re:<span>Fail</span></Link><Link href="/posts/new">새 기록</Link></header>
    <section className={styles.title}><span>MY ARCHIVE</span><h1>내 기록 보관함</h1><p>익명으로 남긴 기록과 그 이후의 변화까지 한곳에서 관리합니다.</p></section>
    {loading ? <p className={styles.empty}>기록을 불러오는 중입니다.</p> : posts.length === 0 ? <p className={styles.empty}>아직 작성한 기록이 없습니다.</p> :
      <div className={styles.grid}>{posts.map((post) => <article key={post.postId}>
        <div><span>{post.categoryName}</span><span>#{post.emotionTag}</span></div>
        <Link href={`/posts/${post.postId}`}><h2>{post.title}</h2><p>{post.summary}</p></Link>
        <footer><span>공감 {post.reactionCount}</span>{post.hasUpdates && <strong>후속 기록 있음</strong>}<Link href={`/posts/${post.postId}/edit`}>수정</Link></footer>
      </article>)}</div>}
  </main>;
}
