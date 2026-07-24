"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { getMyPosts } from "@/lib/api/posts";
import type { PostSummary } from "@/types/post";
import styles from "./page.module.css";

export default function MyRecordsPage() {
  const auth = useRequireAuth();
  const [posts, setPosts] = useState<PostSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!auth.ready || !auth.token) return;
    getMyPosts(auth.token)
      .then((page) => setPosts(page.content))
      .catch(() => setError("기록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."))
      .finally(() => setLoading(false));
  }, [auth.ready, auth.token]);

  return <main className={styles.page}>
    <header><Link href="/" className={styles.brand}>Re:<span>Fail</span></Link><Link href="/posts/new">새 기록</Link></header>
    <section className={styles.title}><span>MY ARCHIVE</span><h1>내 기록 보관함</h1><p>익명으로 남긴 기록과 그 이후의 변화까지 한곳에서 관리합니다.</p></section>
    {loading ? <p className={styles.empty}>기록을 불러오는 중입니다.</p> : error ? <p className={styles.empty}>{error}</p> : posts.length === 0 ? <p className={styles.empty}>아직 작성한 기록이 없습니다.</p> :
      <div className={styles.grid}>{posts.map((post) => <article key={post.postId}>
        <div><span>{post.categoryName}</span><span>#{post.emotionTag}</span></div>
        <Link href={`/posts/${post.postId}`}><h2>{post.title}</h2><p>{post.summary}</p></Link>
        <footer><span>공감 {post.reactionCount}</span>{post.hasUpdates && <strong>후속 기록 있음</strong>}<Link href={`/posts/${post.postId}/edit`}>수정</Link></footer>
      </article>)}</div>}
  </main>;
}
