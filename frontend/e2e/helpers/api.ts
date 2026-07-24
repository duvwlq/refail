import { randomUUID } from "node:crypto";
import type { APIRequestContext } from "@playwright/test";

const API_BASE_URL = process.env.E2E_API_BASE_URL ?? "http://localhost:18080/api/v1";
export const E2E_PASSWORD = process.env.E2E_USER_PASSWORD ?? "e2e-password-123!";
export const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? "admin@refail.e2e";
export const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? E2E_PASSWORD;

type LoginResponse = {
  accessToken: string;
  user: { userId: number };
};

type Category = {
  categoryId: number;
  slug: string;
};

export type TestUser = {
  email: string;
  nickname: string;
  password: string;
  token: string;
  userId: number;
};

export function uniqueValue(prefix: string) {
  return `${prefix}-${Date.now()}-${randomUUID().slice(0, 8)}`;
}

export async function createUser(
  request: APIRequestContext,
  prefix: string,
): Promise<TestUser> {
  const unique = uniqueValue(prefix);
  const email = `${unique}@refail.e2e`;
  const nickname = unique.slice(0, 30);
  const signup = await request.post(`${API_BASE_URL}/auth/signup`, {
    data: { email, nickname, password: E2E_PASSWORD },
  });
  assertResponse(signup.ok(), `회원가입 실패: ${signup.status()}`);

  const session = await login(request, email, E2E_PASSWORD);
  return {
    email,
    nickname,
    password: E2E_PASSWORD,
    token: session.accessToken,
    userId: session.user.userId,
  };
}

export async function login(
  request: APIRequestContext,
  email: string,
  password: string,
): Promise<LoginResponse> {
  const response = await request.post(`${API_BASE_URL}/auth/login`, {
    data: { email, password },
  });
  assertResponse(response.ok(), `로그인 실패: ${response.status()}`);
  return response.json() as Promise<LoginResponse>;
}

export async function createPost(
  request: APIRequestContext,
  user: TestUser,
  title: string,
  categorySlug = "study",
) {
  const categoriesResponse = await request.get(`${API_BASE_URL}/categories`);
  assertResponse(categoriesResponse.ok(), `카테고리 조회 실패: ${categoriesResponse.status()}`);
  const categories = await categoriesResponse.json() as Category[];
  const category = categories.find((item) => item.slug === categorySlug);
  assertResponse(Boolean(category), `카테고리를 찾지 못함: ${categorySlug}`);

  const response = await request.post(`${API_BASE_URL}/posts`, {
    headers: authorization(user.token),
    data: {
      categoryId: category!.categoryId,
      title,
      content: "## 무엇을 시도했나요?\n\nE2E에서 핵심 흐름을 검증합니다.",
      visibilityType: "NICKNAME",
      failureSize: "SMALL",
      emotionTag: "테스트",
      advicePreference: "COMFORT",
      retryIntention: true,
      nextAttemptPlan: "다음에는 더 작은 범위부터 시작합니다.",
    },
  });
  assertResponse(response.ok(), `게시글 생성 실패: ${response.status()}`);
  return response.json() as Promise<{ postId: number }>;
}

export async function reportPost(
  request: APIRequestContext,
  reporter: TestUser,
  postId: number,
) {
  const response = await request.post(`${API_BASE_URL}/posts/${postId}/reports`, {
    headers: authorization(reporter.token),
    data: {
      reasonType: "SPAM",
      reasonDetail: "Playwright 관리자 moderation 검증",
    },
  });
  assertResponse(response.ok(), `신고 생성 실패: ${response.status()}`);
  return response.json() as Promise<{ reportId: number }>;
}

export function apiUrl(path: string) {
  return `${API_BASE_URL}${path}`;
}

function authorization(token: string) {
  return { Authorization: `Bearer ${token}` };
}

function assertResponse(condition: boolean, message: string): asserts condition {
  if (!condition) throw new Error(message);
}
