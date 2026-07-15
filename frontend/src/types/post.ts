export type PostSummary = {
  postId: number;
  categoryId: number;
  categoryName: string;
  title: string;
  summary: string;
  visibilityType: "ANONYMOUS" | "NICKNAME";
  authorName: string;
  failureSize: "SMALL" | "MEDIUM" | "LARGE";
  emotionTag: string;
  reactionCount: number;
  hasUpdates: boolean;
  createdAt: string;
};

export type PageResponse<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Category = {
  categoryId: number;
  name: string;
  slug: string;
  displayOrder: number;
};

export type CreatedPost = {
  postId: number;
  title: string;
};

export type PostUpdate = {
  updateId: number;
  status: string;
  content: string;
  createdAt: string;
};

export type PostDetail = {
  postId: number;
  categoryId: number;
  categoryName: string;
  title: string;
  content: string;
  visibilityType: "ANONYMOUS" | "NICKNAME";
  authorName: string;
  failureSize: "SMALL" | "MEDIUM" | "LARGE";
  emotionTag: string;
  advicePreference: "COMFORT" | "ADVICE_OK";
  retryIntention: boolean;
  nextAttemptPlan: string | null;
  reactionCount: number;
  updates: PostUpdate[];
  createdAt: string;
  updatedAt: string;
};

export type PostOwnership = {
  postId: number;
  ownedByMe: boolean;
};

export type ReactionState = {
  postId: number;
  reactionType: "ME_TOO" | "SEND_SUPPORT" | "THANKS_FOR_SHARING" | "CHEERING_NEXT_TRY" | null;
  applied: boolean;
};
