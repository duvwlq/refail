"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { MarkdownContent } from "@/components/markdown/MarkdownContent";
import { ApiError, apiFetch } from "@/lib/api";
import { getAccessToken } from "@/lib/auth";
import type { PostDetail, PostOwnership, ReactionState, PostUpdate } from "@/types/post";
import styles from "./PostDetailView.module.css";

type ReactionType = "ME_TOO" | "SEND_SUPPORT" | "THANKS_FOR_SHARING" | "CHEERING_NEXT_TRY";
type ReportReason = "ABUSE" | "HATE" | "SPAM" | "PRIVACY" | "OTHER";
type ReactionCount = { reactionType: ReactionType; count: number };

const reactions: { type: ReactionType; label: string }[] = [
  { type: "ME_TOO", label: "나도 그랬어요" },
  { type: "SEND_SUPPORT", label: "응원할게요" },
  { type: "THANKS_FOR_SHARING", label: "공유해줘서 고마워요" },
  { type: "CHEERING_NEXT_TRY", label: "다음 시도를 응원해요" },
];

const failureSizeLabel = { SMALL: "작은 걸림돌", MEDIUM: "제법 큰 실패", LARGE: "큰 실패" } as const;
const updateStatusLabel: Record<string, string> = { TRYING_AGAIN: "다시 시도 중", STILL_FAILING: "잠시 멈춤", IMPROVING: "조금씩 나아지는 중", SUCCEEDED: "극복함" };

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { year: "numeric", month: "long", day: "numeric" }).format(new Date(value));
}

export function PostDetailView({ post }: { post: PostDetail }) {
  const router = useRouter();
  const [selectedReaction, setSelectedReaction] = useState<ReactionType | null>(null);
  const [reactionCount, setReactionCount] = useState(post.reactionCount);
  const [reactionPending, setReactionPending] = useState(false);
  const [reactionMessage, setReactionMessage] = useState("");
  const [reactionCounts, setReactionCounts] = useState<Record<string, number>>({});
  const [ownedByMe, setOwnedByMe] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletePending, setDeletePending] = useState(false);
  const [updates, setUpdates] = useState(post.updates);
  const [updateOpen, setUpdateOpen] = useState(false);
  const [updateStatus, setUpdateStatus] = useState("TRYING_AGAIN");
  const [updateContent, setUpdateContent] = useState("");
  const [editingUpdateId, setEditingUpdateId] = useState<number | null>(null);
  const [updatePending, setUpdatePending] = useState(false);
  const [reportOpen, setReportOpen] = useState(false);
  const [reportReason, setReportReason] = useState<ReportReason>("ABUSE");
  const [reportDetail, setReportDetail] = useState("");
  const [reportMessage, setReportMessage] = useState("");
  const [reportPending, setReportPending] = useState(false);

  useEffect(() => {
    const token = getAccessToken();
    apiFetch<ReactionCount[]>(`/api/v1/posts/${post.postId}/reaction/summary`)
      .then((items) => setReactionCounts(Object.fromEntries(items.map((item) => [item.reactionType, item.count]))));
    if (!token) return;

    apiFetch<PostOwnership>(`/api/v1/posts/${post.postId}/ownership`, { token })
      .then((ownership) => setOwnedByMe(ownership.ownedByMe))
      .catch(() => setOwnedByMe(false));
    apiFetch<ReactionState>(`/api/v1/posts/${post.postId}/reaction`, { token })
      .then((reaction) => setSelectedReaction(reaction.applied ? reaction.reactionType : null))
      .catch(() => setSelectedReaction(null));
  }, [post.postId]);
  const orderedReactions = [...reactions].sort((a, b) => reactionPriority(a.type, post.retryIntention) - reactionPriority(b.type, post.retryIntention));

  function requireToken() {
    const token = getAccessToken();
    if (!token) router.push("/login");
    return token;
  }

  async function handleReaction(type: ReactionType) {
    const token = requireToken();
    if (!token || reactionPending) return;
    setReactionPending(true);
    setReactionMessage("");
    try {
      if (selectedReaction === type) {
        await apiFetch(`/api/v1/posts/${post.postId}/reaction`, { method: "DELETE", token });
        setSelectedReaction(null);
        setReactionCount((count) => Math.max(0, count - 1));
      } else {
        await apiFetch(`/api/v1/posts/${post.postId}/reaction`, {
          method: "PUT", token, body: JSON.stringify({ reactionType: type }),
        });
        if (!selectedReaction) setReactionCount((count) => count + 1);
        setSelectedReaction(type);
      }
    } catch (error) {
      setReactionMessage(error instanceof ApiError ? error.message : "공감을 저장하지 못했습니다.");
    } finally {
      setReactionPending(false);
    }
  }

  async function handleDelete() {
    const token = requireToken();
    if (!token || deletePending) return;
    setDeletePending(true);
    try {
      await apiFetch(`/api/v1/posts/${post.postId}`, { method: "DELETE", token });
      router.push("/");
      router.refresh();
    } catch (error) {
      setReactionMessage(error instanceof ApiError ? error.message : "기록을 삭제하지 못했습니다.");
      setDeleteOpen(false);
      setDeletePending(false);
    }
  }

  async function saveUpdate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const token = requireToken();
    if (!token) return;
    setUpdatePending(true);
    try {
      const path = editingUpdateId ? `/api/v1/posts/${post.postId}/updates/${editingUpdateId}` : `/api/v1/posts/${post.postId}/updates`;
      const saved = await apiFetch<PostUpdate>(path, { method: editingUpdateId ? "PATCH" : "POST", token, body: JSON.stringify({ status: updateStatus, content: updateContent }) });
      setUpdates((items) => editingUpdateId ? items.map((item) => item.updateId === saved.updateId ? saved : item) : [...items, saved]);
      setUpdateContent(""); setEditingUpdateId(null); setUpdateOpen(false);
    } finally { setUpdatePending(false); }
  }

  function editUpdate(update: PostUpdate) {
    setEditingUpdateId(update.updateId); setUpdateStatus(update.status); setUpdateContent(update.content); setUpdateOpen(true);
  }

  async function deleteUpdate(updateId: number) {
    const token = requireToken(); if (!token) return;
    await apiFetch(`/api/v1/posts/${post.postId}/updates/${updateId}`, { method: "DELETE", token });
    setUpdates((items) => items.filter((item) => item.updateId !== updateId));
  }

  async function handleReport(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const token = requireToken();
    if (!token) return;
    setReportPending(true);
    setReportMessage("");
    try {
      await apiFetch(`/api/v1/posts/${post.postId}/reports`, {
        method: "POST", token,
        body: JSON.stringify({ reasonType: reportReason, reasonDetail: reportDetail.trim() || null }),
      });
      setReportMessage("신고가 접수되었습니다. 운영자가 확인할게요.");
    } catch (error) {
      setReportMessage(error instanceof ApiError ? error.message : "신고를 접수하지 못했습니다.");
    } finally {
      setReportPending(false);
    }
  }

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <Link href="/" className={styles.brand}>Re:<span>Fail</span></Link>
        <Link href="/" className={styles.back}>기록 목록</Link>
      </header>

      <article className={styles.article}>
        <div className={styles.heading}>
          <div className={styles.tags}>
            <span>{post.categoryName}</span><span>{failureSizeLabel[post.failureSize]}</span><span>#{post.emotionTag}</span>
          </div>
          <h1>{post.title}</h1>
          <div className={styles.headingFooter}>
            <div className={styles.meta}><strong>{post.authorName}</strong><span>·</span><time>{formatDate(post.createdAt)}</time></div>
            {ownedByMe && (
              <div className={styles.ownerActions}>
                <Link href={`/posts/${post.postId}/edit`}>수정</Link>
                <button type="button" onClick={() => setDeleteOpen(true)}>삭제</button>
              </div>
            )}
          </div>
        </div>

        <MarkdownContent content={post.content} />

        {post.nextAttemptPlan && (
          <section className={styles.nextPlan}>
            <span>NEXT CHOICE</span>
            <h2>다음에는 이렇게 해볼 거예요.</h2>
            <p>{post.nextAttemptPlan}</p>
          </section>
        )}

        {ownedByMe && <section className={styles.updateComposer}>
          <div><div><span>GROWTH LOG</span><h2>그 후에는 어떻게 되었나요?</h2></div><button type="button" onClick={() => { setUpdateOpen((open) => !open); setEditingUpdateId(null); setUpdateContent(""); }}>후속 기록 작성</button></div>
          {updateOpen && <form onSubmit={saveUpdate}>
            <select value={updateStatus} onChange={(event) => setUpdateStatus(event.target.value)}><option value="TRYING_AGAIN">다시 시도 중</option><option value="STILL_FAILING">잠시 멈춤</option><option value="IMPROVING">조금씩 나아지는 중</option><option value="SUCCEEDED">극복함</option></select>
            <textarea required rows={5} value={updateContent} onChange={(event) => setUpdateContent(event.target.value)} placeholder="실패 이후 달라진 선택과 과정을 적어보세요." />
            <button disabled={updatePending}>{updatePending ? "저장 중..." : editingUpdateId ? "수정 완료" : "후속 기록 남기기"}</button>
          </form>}
        </section>}

        {updates.length > 0 && (
          <section className={styles.updates}>
            <p className={styles.sectionLabel}>이후의 기록</p>
            {updates.map((update) => (
              <article className={styles.update} key={update.updateId}>
                <div><strong>{updateStatusLabel[update.status] ?? update.status}</strong><span><time>{formatDate(update.createdAt)}</time>{ownedByMe && <><button type="button" onClick={() => editUpdate(update)}>수정</button><button type="button" onClick={() => deleteUpdate(update.updateId)}>삭제</button></>}</span></div>
                <MarkdownContent content={update.content} />
              </article>
            ))}
          </section>
        )}

        <section className={styles.reactionSection}>
          <div><span className={styles.sectionLabel}>말 대신 건네는 반응</span><strong>공감 {reactionCount}</strong></div>
          <div className={styles.reactions}>
            {orderedReactions.map((reaction) => (
              <button key={reaction.type} type="button" data-selected={selectedReaction === reaction.type} disabled={reactionPending || ownedByMe} onClick={() => handleReaction(reaction.type)}>
                {reaction.label} <small>{reactionCounts[reaction.type] ?? 0}</small>
              </button>
            ))}
          </div>
          <p>{ownedByMe ? "내 기록에는 공감을 남길 수 없습니다." : "댓글은 운영하지 않습니다. 판단보다 안전한 공감을 남겨 주세요."}</p>
          {reactionMessage && <p className={styles.actionMessage}>{reactionMessage}</p>}
        </section>

        <div className={styles.reportArea}>
          <button type="button" onClick={() => setReportOpen((open) => !open)}>이 기록 신고하기</button>
          {reportOpen && (
            <form className={styles.reportForm} onSubmit={handleReport}>
              <label>신고 이유
                <select value={reportReason} onChange={(event) => setReportReason(event.target.value as ReportReason)}>
                  <option value="ABUSE">욕설·괴롭힘</option><option value="HATE">혐오 표현</option><option value="SPAM">스팸·홍보</option><option value="PRIVACY">개인정보 노출</option><option value="OTHER">기타</option>
                </select>
              </label>
              <label>상세 내용 <span>선택</span>
                <textarea value={reportDetail} onChange={(event) => setReportDetail(event.target.value)} maxLength={500} rows={3} />
              </label>
              {reportMessage && <p>{reportMessage}</p>}
              <button type="submit" disabled={reportPending}>{reportPending ? "접수 중..." : "신고 접수"}</button>
            </form>
          )}
        </div>
      </article>

      {deleteOpen && (
        <div className={styles.modalBackdrop} role="presentation" onMouseDown={() => !deletePending && setDeleteOpen(false)}>
          <section className={styles.deleteModal} role="dialog" aria-modal="true" aria-labelledby="delete-title" onMouseDown={(event) => event.stopPropagation()}>
            <span>기록 삭제</span>
            <h2 id="delete-title">이 기록을 정말 삭제할까요?</h2>
            <p>삭제한 기록과 후속 기록은 목록과 상세 화면에서 더 이상 볼 수 없습니다.</p>
            <div>
              <button type="button" onClick={() => setDeleteOpen(false)} disabled={deletePending}>취소</button>
              <button type="button" onClick={handleDelete} disabled={deletePending}>{deletePending ? "삭제 중..." : "삭제하기"}</button>
            </div>
          </section>
        </div>
      )}
    </main>
  );
}

function reactionPriority(type: ReactionType, retryIntention: boolean) {
  if (retryIntention && type === "CHEERING_NEXT_TRY") return -1;
  return type === "ME_TOO" ? 0 : type === "SEND_SUPPORT" ? 1 : type === "THANKS_FOR_SHARING" ? 2 : 3;
}
