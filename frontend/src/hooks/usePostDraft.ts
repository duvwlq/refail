"use client";

import { useEffect, useEffectEvent, useState } from "react";

const DRAFT_VERSION = 1;

export type PostDraft = {
  title: string;
  content: string;
  emotionTag: string;
  nextAttemptPlan: string;
  failureSize: "SMALL" | "MEDIUM" | "LARGE";
  advicePreference: "COMFORT" | "ADVICE_OK";
  retryIntention: boolean;
};

type StoredDraft = {
  version: typeof DRAFT_VERSION;
  value: PostDraft;
};

export function usePostDraft(
  key: string,
  value: PostDraft,
  onRestore: (draft: PostDraft) => void,
) {
  const [ready, setReady] = useState(false);
  const [notice, setNotice] = useState("");
  const restore = useEffectEvent(onRestore);
  const serializedValue = JSON.stringify(value);

  useEffect(() => {
    const raw = window.localStorage.getItem(key);
    let restoredDraft: PostDraft | null = null;
    if (raw) {
      try {
        const stored = JSON.parse(raw) as Partial<StoredDraft>;
        if (stored.version === DRAFT_VERSION && isPostDraft(stored.value)) {
          restoredDraft = stored.value;
        } else {
          window.localStorage.removeItem(key);
        }
      } catch {
        window.localStorage.removeItem(key);
      }
    }
    const restoreTimer = window.setTimeout(() => {
      if (restoredDraft) {
        restore(restoredDraft);
        setNotice("자동 저장된 작성 내용을 복원했습니다.");
      }
      setReady(true);
    }, 0);
    return () => window.clearTimeout(restoreTimer);
  }, [key]);

  useEffect(() => {
    if (!ready) return;
    const timer = window.setTimeout(() => {
      const stored: StoredDraft = {
        version: DRAFT_VERSION,
        value: JSON.parse(serializedValue) as PostDraft,
      };
      window.localStorage.setItem(key, JSON.stringify(stored));
    }, 500);
    return () => window.clearTimeout(timer);
  }, [key, ready, serializedValue]);

  return {
    notice,
    clear: () => window.localStorage.removeItem(key),
  };
}

function isPostDraft(value: unknown): value is PostDraft {
  if (!value || typeof value !== "object") return false;
  const draft = value as Partial<PostDraft>;
  return (
    typeof draft.title === "string"
    && typeof draft.content === "string"
    && typeof draft.emotionTag === "string"
    && typeof draft.nextAttemptPlan === "string"
    && ["SMALL", "MEDIUM", "LARGE"].includes(draft.failureSize ?? "")
    && ["COMFORT", "ADVICE_OK"].includes(draft.advicePreference ?? "")
    && typeof draft.retryIntention === "boolean"
  );
}
