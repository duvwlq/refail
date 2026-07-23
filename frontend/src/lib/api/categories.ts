import { apiFetch } from "@/lib/api";
import type { Category } from "@/types/post";

export function getCategories(init: RequestInit = {}): Promise<Category[]> {
  return apiFetch<Category[]>("/api/v1/categories", init);
}
