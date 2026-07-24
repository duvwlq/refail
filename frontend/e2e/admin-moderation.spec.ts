import { expect, test } from "@playwright/test";
import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  apiUrl,
  createPost,
  createUser,
  reportPost,
  uniqueValue,
} from "./helpers/api";

test("관리자가 신고 게시글을 숨기고 공개 제외 후 복구한다", async ({ page, request }) => {
  const author = await createUser(request, "moderation-author");
  const reporter = await createUser(request, "moderation-reporter");
  const title = uniqueValue("관리자 숨김 검증 기록");
  const post = await createPost(request, author, title, "daily");
  const report = await reportPost(request, reporter, post.postId);

  await page.goto("/login");
  await page.getByLabel("이메일").fill(ADMIN_EMAIL);
  await page.getByLabel("비밀번호").fill(ADMIN_PASSWORD);
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page).toHaveURL("/");

  await page.goto("/admin");
  const pendingReport = page.getByTestId(`report-${report.reportId}`);
  await expect(pendingReport).toContainText(`게시글 #${post.postId}`);
  await pendingReport.getByRole("button", { name: "숨김" }).click();
  await expect(pendingReport).toHaveCount(0);

  const hiddenResponse = await request.get(apiUrl(`/posts/${post.postId}`));
  expect(hiddenResponse.status()).toBe(404);

  await page.getByLabel("신고 상태").selectOption("RESOLVED");
  const resolvedReport = page.getByTestId(`report-${report.reportId}`);
  await expect(resolvedReport).toBeVisible();
  await resolvedReport.getByRole("button", { name: "복구" }).click();

  await expect.poll(async () => (await request.get(apiUrl(`/posts/${post.postId}`))).status())
    .toBe(200);
});
