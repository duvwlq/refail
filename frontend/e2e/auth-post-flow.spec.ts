import { expect, test } from "@playwright/test";
import { createUser, uniqueValue } from "./helpers/api";

test("로그인 후 게시글 작성·수정·후속 기록과 내 기록 조회가 이어진다", async ({ page, request }) => {
  const user = await createUser(request, "post-author");
  const originalTitle = uniqueValue("작성 흐름 실패 기록");
  const editedTitle = `${originalTitle} 수정`;

  await page.goto("/login");
  await page.getByLabel("이메일").fill(user.email);
  await page.getByLabel("비밀번호").fill(user.password);
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page).toHaveURL("/");

  await page.goto("/posts/new");
  await page.getByLabel("카테고리").selectOption({ label: "공부" });
  await page.getByLabel("지금 감정").fill("아쉬움");
  await page.getByRole("textbox", { name: "제목", exact: false }).fill(originalTitle);
  await page.getByLabel("글쓰기").fill("## 무엇을 시도했나요?\n\nPlaywright로 작성 흐름을 검증했습니다.");
  await page.getByRole("radio", { name: "닉네임으로", exact: false }).check();
  await page.getByLabel("다음에는 어떤 선택을 해볼까요?", { exact: false })
    .fill("다음에는 작은 검증부터 시작합니다.");
  await page.getByRole("button", { name: "기록 발행하기" }).click();

  await expect(page).toHaveURL(/\/posts\/\d+$/);
  await expect(page.getByRole("heading", { name: originalTitle })).toBeVisible();

  await page.getByRole("link", { name: "수정" }).click();
  await page.getByRole("textbox", { name: "제목", exact: false }).fill(editedTitle);
  await page.getByRole("button", { name: "수정 완료" }).click();
  await expect(page.getByRole("heading", { name: editedTitle })).toBeVisible();

  await page.getByRole("button", { name: "후속 기록 작성" }).click();
  await page.getByLabel("후속 상태").selectOption("SUCCEEDED");
  await page.getByLabel("후속 기록 내용").fill("작은 범위부터 검증해 흐름을 완성했습니다.");
  await page.getByRole("button", { name: "후속 기록 남기기" }).click();
  await expect(page.getByText("극복함", { exact: true })).toBeVisible();

  await page.goto("/my-records");
  await expect(page.getByRole("heading", { name: editedTitle })).toBeVisible();
});
