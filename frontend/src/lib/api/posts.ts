import { apiFetch } from "@/lib/api";
import type {
  CreatedPost,
  PostDetail,
  PostOwnership,
  PageResponse,
  PostSummary,
  PostUpdate,
  ReactionState,
} from "@/types/post";

export type ReactionType =
  | "ME_TOO"
  | "SEND_SUPPORT"
  | "THANKS_FOR_SHARING"
  | "CHEERING_NEXT_TRY";

export type ReactionCount = {
  reactionType: ReactionType;
  count: number;
};

export type ReportReason = "ABUSE" | "HATE" | "SPAM" | "PRIVACY" | "OTHER";

export type PostWritePayload = {
  categoryId: number;
  title: string;
  content: string;
  failureSize: "SMALL" | "MEDIUM" | "LARGE";
  emotionTag: string;
  advicePreference: "COMFORT" | "ADVICE_OK";
  retryIntention: boolean;
  nextAttemptPlan: string | null;
  visibilityType?: "ANONYMOUS" | "NICKNAME";
};

export type PostSearch = {
  sort?: string;
  categoryId?: string;
  failureSize?: string;
  keyword?: string;
  page?: string;
};

export function searchPosts(
  search: PostSearch,
  init: RequestInit = {},
): Promise<PageResponse<PostSummary>> {
  const query = new URLSearchParams({
    sort: search.sort ?? "latest",
    page: search.page ?? "0",
    size: "6",
  });
  if (search.categoryId) query.set("categoryId", search.categoryId);
  if (search.failureSize) query.set("failureSize", search.failureSize);
  if (search.keyword) query.set("keyword", search.keyword);
  return apiFetch<PageResponse<PostSummary>>(`/api/v1/posts?${query}`, init);
}

export function getPost(postId: number, init: RequestInit = {}): Promise<PostDetail> {
  return apiFetch<PostDetail>(`/api/v1/posts/${postId}`, init);
}

export function getMyPosts(token: string): Promise<PageResponse<PostSummary>> {
  return apiFetch<PageResponse<PostSummary>>("/api/v1/posts/me?page=0&size=50", { token });
}

export function getPostOwnership(postId: number, token: string): Promise<PostOwnership> {
  return apiFetch<PostOwnership>(`/api/v1/posts/${postId}/ownership`, { token });
}

export function createPost(payload: PostWritePayload, token: string): Promise<CreatedPost> {
  return apiFetch<CreatedPost>("/api/v1/posts", {
    method: "POST",
    token,
    body: JSON.stringify(payload),
  });
}

export function updatePost(
  postId: number,
  payload: PostWritePayload,
  token: string,
): Promise<CreatedPost> {
  return apiFetch<CreatedPost>(`/api/v1/posts/${postId}`, {
    method: "PATCH",
    token,
    body: JSON.stringify(payload),
  });
}

export function deletePost(postId: number, token: string): Promise<void> {
  return apiFetch(`/api/v1/posts/${postId}`, { method: "DELETE", token });
}

export function getReactionSummary(postId: number): Promise<ReactionCount[]> {
  return apiFetch<ReactionCount[]>(`/api/v1/posts/${postId}/reaction/summary`);
}

export function getMyReaction(postId: number, token: string): Promise<ReactionState> {
  return apiFetch<ReactionState>(`/api/v1/posts/${postId}/reaction`, { token });
}

export function setReaction(postId: number, reactionType: ReactionType, token: string): Promise<void> {
  return apiFetch(`/api/v1/posts/${postId}/reaction`, {
    method: "PUT",
    token,
    body: JSON.stringify({ reactionType }),
  });
}

export function removeReaction(postId: number, token: string): Promise<void> {
  return apiFetch(`/api/v1/posts/${postId}/reaction`, { method: "DELETE", token });
}

export function savePostUpdate(
  postId: number,
  updateId: number | null,
  payload: Pick<PostUpdate, "status" | "content">,
  token: string,
): Promise<PostUpdate> {
  const path = updateId
    ? `/api/v1/posts/${postId}/updates/${updateId}`
    : `/api/v1/posts/${postId}/updates`;
  return apiFetch<PostUpdate>(path, {
    method: updateId ? "PATCH" : "POST",
    token,
    body: JSON.stringify(payload),
  });
}

export function deletePostUpdate(postId: number, updateId: number, token: string): Promise<void> {
  return apiFetch(`/api/v1/posts/${postId}/updates/${updateId}`, {
    method: "DELETE",
    token,
  });
}

export function reportPost(
  postId: number,
  payload: { reasonType: ReportReason; reasonDetail: string | null },
  token: string,
): Promise<void> {
  return apiFetch(`/api/v1/posts/${postId}/reports`, {
    method: "POST",
    token,
    body: JSON.stringify(payload),
  });
}
