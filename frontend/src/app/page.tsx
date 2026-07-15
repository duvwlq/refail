import Link from "next/link";
import { apiFetch } from "@/lib/api";
import type { Category, PageResponse, PostSummary } from "@/types/post";
import { AuthNav } from "@/components/AuthNav";
import styles from "./page.module.css";

const failureSizeLabel = {
  SMALL: "작은 걸림돌",
  MEDIUM: "제법 큰 실패",
  LARGE: "큰 실패",
} as const;

type Search = { sort?: string; categoryId?: string; failureSize?: string; keyword?: string; page?: string };

async function getPosts(search: Search): Promise<{ result: PageResponse<PostSummary> | null; connected: boolean }> {
  try {
    const query = new URLSearchParams({ sort: search.sort ?? "latest", page: search.page ?? "0", size: "6" });
    if (search.categoryId) query.set("categoryId", search.categoryId);
    if (search.failureSize) query.set("failureSize", search.failureSize);
    if (search.keyword) query.set("keyword", search.keyword);
    const response = await apiFetch<PageResponse<PostSummary>>(
      `/api/v1/posts?${query}`,
      { cache: "no-store" },
    );
    return { result: response, connected: true };
  } catch {
    return { result: null, connected: false };
  }
}

async function getCategories() { try { return await apiFetch<Category[]>("/api/v1/categories", { cache: "no-store" }); } catch { return []; } }

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
  }).format(new Date(value));
}

export default async function Home({ searchParams }: { searchParams: Promise<Search> }) {
  const search = await searchParams;
  const [{ result, connected }, categories] = await Promise.all([getPosts(search), getCategories()]);
  const posts = result?.content ?? [];

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <Link href="/" className={styles.brand}>
          Re:<span>Fail</span>
        </Link>
        <AuthNav />
      </header>

      <section className={styles.hero}>
        <div className={styles.heroCopy}>
          <p className={styles.eyebrow}>FAILURE ARCHIVE · 001</p>
          <h1>
            잘된 일 말고,
            <br />잘 안된 일을 꺼내놓는 곳.
          </h1>
          <p className={styles.heroDescription}>
            실패는 결론이 아니라 다음 시도를 위한 기록입니다. 거창한 좌절도,
            오늘 야식을 참지 못한 일도 괜찮습니다.
          </p>
          <div className={styles.heroActions}>
            <Link href="/posts/new" className={styles.primaryAction}>내 실패 기록하기</Link>
            <Link href="#records" className={styles.textAction}>다른 기록 먼저 보기 ↓</Link>
          </div>
        </div>
        <aside className={styles.manifesto}>
          <span className={styles.manifestoNumber}>01</span>
          <p>성공은 결과를 보여주고</p>
          <p>실패는 과정을 보여준다.</p>
          <div className={styles.manifestoRule} />
          <small>비교 대신 관찰을, 자책 대신 다음 방법을.</small>
        </aside>
      </section>

      <section className={styles.feed} id="records">
        <div className={styles.feedHeader}>
          <div>
            <p className={styles.sectionIndex}>최근 보관된 기록</p>
            <h2>오늘의 실패들</h2>
          </div>
          <div className={styles.connection} data-connected={connected}>
            <span /> {connected ? "백엔드 연결됨" : "백엔드 연결 대기"}
          </div>
        </div>

        <form className={styles.filters} action="/">
          <input name="keyword" defaultValue={search.keyword} placeholder="제목, 본문, 감정 태그 검색" />
          <select name="categoryId" defaultValue={search.categoryId ?? ""}><option value="">모든 카테고리</option>{categories.map((category) => <option key={category.categoryId} value={category.categoryId}>{category.name}</option>)}</select>
          <select name="failureSize" defaultValue={search.failureSize ?? ""}><option value="">모든 크기</option><option value="SMALL">작은 걸림돌</option><option value="MEDIUM">제법 큰 실패</option><option value="LARGE">큰 실패</option></select>
          <select name="sort" defaultValue={search.sort ?? "latest"}><option value="latest">최신순</option><option value="popular">공감순</option></select>
          <button>검색</button><Link href="/#records">초기화</Link>
        </form>

        {posts.length > 0 ? (
          <div className={styles.grid}>
            {posts.map((post, index) => (
              <article className={styles.card} key={post.postId}>
                <Link href={`/posts/${post.postId}`} className={styles.cardVisual} data-size={post.failureSize}>
                  <span>{post.categoryName}</span>
                  <strong>{failureSizeLabel[post.failureSize]}</strong>
                </Link>
                <div className={styles.cardTopline}>
                  <span>{String(index + 1).padStart(2, "0")}</span>
                  <span>{post.categoryName}</span>
                  <span>{failureSizeLabel[post.failureSize]}</span>
                </div>
                <Link href={`/posts/${post.postId}`} className={styles.cardLink}>
                  <h3>{post.title}</h3>
                  <p>{post.summary}</p>
                </Link>
                <footer className={styles.cardFooter}>
                  <span className={styles.emotion}>#{post.emotionTag}</span>
                  <span>{post.authorName} · {formatDate(post.createdAt)}</span>
                  <span>공감 {post.reactionCount}</span>
                  {post.hasUpdates && <strong>후속 기록 있음</strong>}
                </footer>
              </article>
            ))}
          </div>
        ) : (
          <div className={styles.empty}>
            <span>아직 비어 있는 첫 번째 서랍</span>
            <h3>{connected ? "첫 실패를 보관해 주세요." : "서버가 연결되면 실패 기록이 여기에 나타납니다."}</h3>
            <p>완벽하게 정리하지 않아도 됩니다. 있었던 일을 솔직하게 적는 것으로 충분합니다.</p>
          </div>
        )}
        {result && result.totalPages > 1 && <nav className={styles.pagination} aria-label="페이지 이동">
          {result.number > 0 && <Link href={{ pathname: "/", query: { ...search, page: result.number - 1 } }}>이전</Link>}
          <span>{result.number + 1} / {result.totalPages}</span>
          {result.number + 1 < result.totalPages && <Link href={{ pathname: "/", query: { ...search, page: result.number + 1 } }}>다음</Link>}
        </nav>}
      </section>

      <footer className={styles.footer}>
        <p>실패를 공유하고, 다음을 준비하다.</p>
        <span>FAILURE ARCHIVE © 2026</span>
      </footer>
    </main>
  );
}
