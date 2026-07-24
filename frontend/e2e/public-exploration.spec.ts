import { expect, test } from "@playwright/test";
import { createPost, createUser, uniqueValue } from "./helpers/api";

test("공개 목록에서 검색·카테고리 필터 후 상세로 이동한다", async ({ page, request }) => {
  const author = await createUser(request, "public-author");
  const title = uniqueValue("공개 탐색 실패 기록");
  const post = await createPost(request, author, title, "study");

  await page.goto("/");
  await expect(page.getByRole("heading", { name: "오늘의 실패들" })).toBeVisible();

  await page.getByPlaceholder("제목, 본문, 감정 태그 검색").fill(title);
  await page.getByLabel("카테고리 필터").selectOption({ label: "공부" });
  await page.getByRole("button", { name: "검색" }).click();

  await expect(page).toHaveURL(/keyword=/);
  const result = page.getByRole("link", { name: title, exact: false });
  await expect(result).toBeVisible();
  await result.click();

  await expect(page).toHaveURL(`/posts/${post.postId}`);
  await expect(page.getByRole("heading", { name: title })).toBeVisible();
});
