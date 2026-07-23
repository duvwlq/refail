import { apiFetch } from "@/lib/api";
import type { Category, PageResponse } from "@/types/post";

export type AdminReport = {
  reportId: number;
  targetId: number;
  reasonType: string;
  reasonDetail: string | null;
  status: string;
  reporterUserId: number;
};

export type OperationMetrics = {
  totalPosts: number;
  updateRate: number;
  pendingReports: number;
  succeededUpdates: number;
};

export type CategoryPayload = {
  name: string;
  slug: string;
  displayOrder: number;
};

export function getReports(status: string, token: string): Promise<PageResponse<AdminReport>> {
  const query = new URLSearchParams({ status, page: "0", size: "50" });
  return apiFetch<PageResponse<AdminReport>>(`/api/v1/admin/reports?${query}`, { token });
}

export function getOperationMetrics(token: string): Promise<OperationMetrics> {
  return apiFetch<OperationMetrics>("/api/v1/admin/metrics", { token });
}

export function moderatePost(
  postId: number,
  action: "hide" | "unhide",
  token: string,
): Promise<void> {
  return apiFetch(`/api/v1/admin/posts/${postId}/${action}`, {
    method: "PATCH",
    token,
    body: action === "hide" ? JSON.stringify({ reason: "신고 검토 후 숨김" }) : undefined,
  });
}

export function saveCategory(
  categoryId: number | null,
  payload: CategoryPayload,
  token: string,
): Promise<Category> {
  return apiFetch<Category>(
    categoryId ? `/api/v1/admin/categories/${categoryId}` : "/api/v1/admin/categories",
    {
      method: categoryId ? "PATCH" : "POST",
      token,
      body: JSON.stringify(payload),
    },
  );
}

export function deactivateCategory(categoryId: number, token: string): Promise<void> {
  return apiFetch(`/api/v1/admin/categories/${categoryId}`, {
    method: "DELETE",
    token,
  });
}
