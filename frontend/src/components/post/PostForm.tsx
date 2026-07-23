"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { MarkdownEditor } from "@/components/markdown/MarkdownEditor";
import { usePostDraft } from "@/hooks/usePostDraft";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { ApiError } from "@/lib/api";
import { getCategories } from "@/lib/api/categories";
import {
  createPost,
  getPostOwnership,
  updatePost,
  type PostWritePayload,
} from "@/lib/api/posts";
import type { Category, PostDetail } from "@/types/post";
import styles from "./PostForm.module.css";

type Choice<T extends string> = { value: T; title: string; description: string };

const visibilityChoices: Choice<"ANONYMOUS" | "NICKNAME">[] = [
  { value: "ANONYMOUS", title: "익명으로", description: "작성자는 나만 알고 다른 사람에게는 익명으로 보여요." },
  { value: "NICKNAME", title: "닉네임으로", description: "내가 정한 닉네임과 함께 기록을 공유해요." },
];

const sizeChoices: Choice<"SMALL" | "MEDIUM" | "LARGE">[] = [
  { value: "SMALL", title: "작은 걸림돌", description: "일상에서 잠깐 멈췄던 일" },
  { value: "MEDIUM", title: "제법 큰 실패", description: "시간이 들었지만 잘되지 않은 일" },
  { value: "LARGE", title: "큰 실패", description: "삶의 방향을 다시 생각하게 한 일" },
];

const adviceChoices: Choice<"COMFORT" | "ADVICE_OK">[] = [
  { value: "COMFORT", title: "공감만 받을게요", description: "해결책보다 따뜻한 공감이 필요해요." },
  { value: "ADVICE_OK", title: "조언도 괜찮아요", description: "다른 사람의 경험과 방법도 듣고 싶어요." },
];

export function PostForm({ initialPost }: { initialPost?: PostDetail }) {
  const router = useRouter();
  const auth = useRequireAuth();
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryId, setCategoryId] = useState(initialPost ? String(initialPost.categoryId) : "");
  const [title, setTitle] = useState(initialPost?.title ?? "");
  const [content, setContent] = useState(initialPost?.content ?? "");
  const [visibilityType, setVisibilityType] = useState<"ANONYMOUS" | "NICKNAME">(initialPost?.visibilityType ?? "ANONYMOUS");
  const [failureSize, setFailureSize] = useState<"SMALL" | "MEDIUM" | "LARGE">(initialPost?.failureSize ?? "SMALL");
  const [emotionTag, setEmotionTag] = useState(initialPost?.emotionTag ?? "");
  const [advicePreference, setAdvicePreference] = useState<"COMFORT" | "ADVICE_OK">(initialPost?.advicePreference ?? "COMFORT");
  const [retryIntention, setRetryIntention] = useState(initialPost?.retryIntention ?? true);
  const [nextAttemptPlan, setNextAttemptPlan] = useState(initialPost?.nextAttemptPlan ?? "");
  const [authorized, setAuthorized] = useState(false);
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const draftKey = `refail.draft.${initialPost?.postId ?? "new"}`;
  const draft = usePostDraft(
    draftKey,
    { title, content, emotionTag, nextAttemptPlan, failureSize, advicePreference, retryIntention },
    (restored) => {
      setTitle(restored.title);
      setContent(restored.content);
      setEmotionTag(restored.emotionTag);
      setNextAttemptPlan(restored.nextAttemptPlan);
      setFailureSize(restored.failureSize);
      setAdvicePreference(restored.advicePreference);
      setRetryIntention(restored.retryIntention);
    },
  );

  useEffect(() => {
    if (!auth.ready || !auth.token) return;
    const token = auth.token;

    async function loadForm() {
      try {
        const items = await getCategories();
        setCategories(items);
        if (!initialPost && items.length > 0) setCategoryId(String(items[0].categoryId));

        if (initialPost) {
          const ownership = await getPostOwnership(initialPost.postId, token);
          if (!ownership.ownedByMe) {
            router.replace(`/posts/${initialPost.postId}`);
            return;
          }
        }
        setAuthorized(true);
      } catch {
        setError("게시글 정보를 불러오지 못했습니다.");
      }
    }
    void loadForm();
  }, [auth.ready, auth.token, initialPost, router]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auth.token) return;
    if (!categoryId) {
      setError("카테고리를 선택해 주세요.");
      return;
    }

    setError("");
    setPending(true);
    try {
      const editableFields: PostWritePayload = {
        categoryId: Number(categoryId),
        title,
        content,
        failureSize,
        emotionTag,
        advicePreference,
        retryIntention,
        nextAttemptPlan: nextAttemptPlan.trim() || null,
      };
      const saved = initialPost
        ? await updatePost(initialPost.postId, editableFields, auth.token)
        : await createPost({ ...editableFields, visibilityType }, auth.token);
      router.push(`/posts/${saved.postId}`);
      draft.clear();
      router.refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "기록을 저장하지 못했습니다.");
    } finally {
      setPending(false);
    }
  }

  if (!authorized) {
    return <div className={styles.loading}>수정 권한을 확인하고 있습니다.</div>;
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <section className={styles.section}>
        <div className={styles.sectionTitle}><b>01</b><h2>어떤 실패인가요?</h2></div>
        <div className={styles.fieldGrid}>
          <label className={styles.field}>
            <span>카테고리</span>
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)} required>
              {categories.length === 0 && <option value="">불러오는 중...</option>}
              {categories.map((category) => <option key={category.categoryId} value={category.categoryId}>{category.name}</option>)}
            </select>
          </label>
          <label className={styles.field}>
            <span>지금 감정</span>
            <input value={emotionTag} onChange={(event) => setEmotionTag(event.target.value)} maxLength={30} required placeholder="예: 아쉬움, 후련함, 막막함" />
          </label>
        </div>
        <ChoiceGroup name="failureSize" label="실패의 크기" choices={sizeChoices} value={failureSize} onChange={setFailureSize} />
      </section>

      <section className={styles.section}>
        <div className={styles.sectionTitle}><b>02</b><h2>무슨 일이 있었나요?</h2></div>
        <label className={styles.field}>
          <span>제목 <small>{title.length}/100</small></span>
          <input className={styles.titleInput} value={title} onChange={(event) => setTitle(event.target.value)} maxLength={100} required placeholder="실패를 한 문장으로 적어보세요" />
        </label>
        <div className={styles.markdownField}>
          <span>내용 <small>마크다운을 지원합니다</small></span>
          <div className={styles.writingGuide}><strong>작성 가이드</strong><span>무엇을 시도했나요?</span><span>어디에서 막혔나요?</span><span>무엇을 알게 되었나요?</span><span>다음에는 어떤 선택을 할까요?</span></div>
          {draft.notice && <p className={styles.draftNotice}>{draft.notice}</p>}
          <MarkdownEditor value={content} onChange={setContent} />
        </div>
      </section>

      <section className={styles.section}>
        <div className={styles.sectionTitle}><b>03</b><h2>어떻게 나눌까요?</h2></div>
        {initialPost ? (
          <p className={styles.visibilityNotice}>공개 방식은 작성 후 변경할 수 없습니다. 현재 {visibilityType === "ANONYMOUS" ? "익명" : "닉네임"}으로 공개 중입니다.</p>
        ) : (
          <ChoiceGroup name="visibilityType" label="공개 방식" choices={visibilityChoices} value={visibilityType} onChange={setVisibilityType} />
        )}
        <ChoiceGroup name="advicePreference" label="받고 싶은 반응" choices={adviceChoices} value={advicePreference} onChange={setAdvicePreference} />
        <label className={styles.retry}>
          <input type="checkbox" checked={retryIntention} onChange={(event) => setRetryIntention(event.target.checked)} />
          <span><strong>다시 시도해 볼 생각이 있어요</strong><small>나중에 후속 기록으로 달라진 과정을 이어갈 수 있습니다.</small></span>
        </label>
        <label className={`${styles.field} ${styles.nextPlan}`}>
          <span>다음에는 어떤 선택을 해볼까요? <small>{nextAttemptPlan.length}/500 · 선택</small></span>
          <textarea
            value={nextAttemptPlan}
            onChange={(event) => setNextAttemptPlan(event.target.value)}
            maxLength={500}
            rows={4}
            placeholder="예: 다음에는 시작 전에 일주일 단위의 작은 목표부터 정해볼 것이다."
          />
        </label>
      </section>

      {error && <p className={styles.error} role="alert">{error}</p>}
      <div className={styles.actions}>
        <button type="button" className={styles.cancel} onClick={() => router.back()}>취소</button>
        <button type="submit" className={styles.submit} disabled={pending || categories.length === 0}>{pending ? "저장하는 중..." : initialPost ? "수정 완료" : "기록 발행하기"}</button>
      </div>
    </form>
  );
}

type ChoiceGroupProps<T extends string> = {
  name: string; label: string; choices: Choice<T>[]; value: T; onChange: (value: T) => void;
};

function ChoiceGroup<T extends string>({ name, label, choices, value, onChange }: ChoiceGroupProps<T>) {
  return (
    <div className={styles.choiceGroup}>
      <span className={styles.groupLabel}>{label}</span>
      <div className={styles.choices}>
        {choices.map((choice) => (
          <label className={styles.choice} data-selected={value === choice.value} key={choice.value}>
            <input type="radio" name={name} value={choice.value} checked={value === choice.value} onChange={() => onChange(choice.value)} />
            <strong>{choice.title}</strong>
            <small>{choice.description}</small>
          </label>
        ))}
      </div>
    </div>
  );
}
