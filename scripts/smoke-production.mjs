import http from "node:http";
import https from "node:https";
import { randomUUID } from "node:crypto";

const args = new Map();
for (let index = 2; index < process.argv.length; index += 2) {
  args.set(process.argv[index], process.argv[index + 1]);
}

const httpPort = Number(args.get("--http-port") ?? "18000");
const httpsPort = Number(args.get("--https-port") ?? "18443");
const httpsAgent = new https.Agent({ keepAlive: true, rejectUnauthorized: false });

function request({
  secure = true,
  path,
  method = "GET",
  headers = {},
  json,
}) {
  const body = json === undefined ? null : JSON.stringify(json);
  const transport = secure ? https : http;
  const requestHeaders = {
    Accept: "application/json",
    ...headers,
  };
  if (body !== null) {
    requestHeaders["Content-Type"] = "application/json";
    requestHeaders["Content-Length"] = Buffer.byteLength(body);
  }

  return new Promise((resolve, reject) => {
    const req = transport.request({
      hostname: "localhost",
      port: secure ? httpsPort : httpPort,
      path,
      method,
      headers: requestHeaders,
      agent: secure ? httpsAgent : undefined,
      rejectUnauthorized: false,
    }, (response) => {
      const chunks = [];
      response.on("data", (chunk) => chunks.push(chunk));
      response.on("end", () => resolve({
        status: response.statusCode ?? 0,
        headers: response.headers,
        body: Buffer.concat(chunks).toString("utf8"),
      }));
    });
    req.on("error", reject);
    if (body !== null) req.write(body);
    req.end();
  });
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function assertStatus(response, expected, step) {
  const accepted = Array.isArray(expected) ? expected : [expected];
  assert(accepted.includes(response.status), `${step}: HTTP ${response.status}`);
}

function parseJson(response, step) {
  try {
    return JSON.parse(response.body);
  } catch {
    throw new Error(`${step}: JSON 응답 형식이 아닙니다.`);
  }
}

function refreshCookieHeader(response, step) {
  const values = response.headers["set-cookie"] ?? [];
  const cookies = Array.isArray(values) ? values : [values];
  const header = cookies.find((value) => value.startsWith("refail_refresh="));
  assert(header, `${step}: Refresh Cookie가 없습니다.`);
  assert(/;\s*Secure/i.test(header), `${step}: Secure 속성이 없습니다.`);
  assert(/;\s*HttpOnly/i.test(header), `${step}: HttpOnly 속성이 없습니다.`);
  assert(/;\s*SameSite=Lax/i.test(header), `${step}: SameSite=Lax 속성이 없습니다.`);
  assert(/;\s*Path=\/api\/v1\/auth/i.test(header), `${step}: Cookie Path가 올바르지 않습니다.`);
  return header.split(";", 1)[0];
}

function logStep(index, message) {
  process.stdout.write(`[${index}/10] ${message}\n`);
}

async function run() {
  const redirect = await request({ secure: false, path: "/api/v1/health" });
  assertStatus(redirect, 308, "HTTP 리다이렉트");
  assert(
    redirect.headers.location === `https://localhost:${httpsPort}/api/v1/health`,
    "HTTP 리다이렉트 Location이 올바르지 않습니다.",
  );
  logStep(1, "HTTP에서 HTTPS로 리다이렉트");

  const home = await request({ path: "/", headers: { Accept: "text/html" } });
  assertStatus(home, 200, "메인 화면");
  assert(home.body.includes("Re:Fail"), "메인 화면에서 서비스 이름을 찾지 못했습니다.");
  logStep(2, "HTTPS 메인 화면과 SSR 백엔드 연결");

  const health = await request({ path: "/api/v1/health" });
  assertStatus(health, 200, "공개 헬스 체크");
  assert(Boolean(health.headers["x-request-id"]), "X-Request-ID가 없습니다.");
  assert(health.headers["strict-transport-security"]?.includes("max-age="), "HSTS가 없습니다.");
  assert(health.headers["x-content-type-options"] === "nosniff", "nosniff 헤더가 없습니다.");
  assert(health.headers["x-frame-options"] === "DENY", "frame 차단 헤더가 없습니다.");
  assert(!health.headers.server, "Server 헤더가 노출되었습니다.");
  logStep(3, "헬스 체크, Request ID와 보안 헤더");

  const swagger = await request({ path: "/v3/api-docs" });
  assertStatus(swagger, 404, "운영 Swagger 비노출");
  logStep(4, "운영 Swagger 비노출");

  const unique = `${Date.now()}-${randomUUID().slice(0, 8)}`;
  const email = `smoke-${unique}@refail.e2e`;
  const password = "smoke-password-123!";
  const nickname = `smoke-${unique}`.slice(0, 30);

  const signup = await request({
    path: "/api/v1/auth/signup",
    method: "POST",
    json: { email, password, nickname },
  });
  assertStatus(signup, [200, 201], "회원가입");
  logStep(5, "고유 테스트 사용자 회원가입");

  const login = await request({
    path: "/api/v1/auth/login",
    method: "POST",
    json: { email, password },
  });
  assertStatus(login, 200, "로그인");
  const loginBody = parseJson(login, "로그인");
  assert(typeof loginBody.accessToken === "string", "로그인 Access Token이 없습니다.");
  const loginCookie = refreshCookieHeader(login, "로그인");
  logStep(6, "로그인과 Secure Refresh Cookie");

  const refresh = await request({
    path: "/api/v1/auth/refresh",
    method: "POST",
    headers: { Cookie: loginCookie },
  });
  assertStatus(refresh, 200, "토큰 갱신");
  const refreshBody = parseJson(refresh, "토큰 갱신");
  assert(typeof refreshBody.accessToken === "string", "갱신 Access Token이 없습니다.");
  const rotatedCookie = refreshCookieHeader(refresh, "토큰 갱신");
  assert(rotatedCookie !== loginCookie, "Refresh Cookie가 회전하지 않았습니다.");
  logStep(7, "Refresh Token 회전");

  const categories = await request({ path: "/api/v1/categories" });
  assertStatus(categories, 200, "카테고리 조회");
  const categoryList = parseJson(categories, "카테고리 조회");
  const category = categoryList.find((item) => item.slug === "study") ?? categoryList[0];
  assert(category?.categoryId, "게시글 작성에 사용할 카테고리가 없습니다.");

  const title = `운영 배포 스모크 ${unique}`;
  const created = await request({
    path: "/api/v1/posts",
    method: "POST",
    headers: { Authorization: `Bearer ${refreshBody.accessToken}` },
    json: {
      categoryId: category.categoryId,
      title,
      content: "## 운영 배포 검증\n\nHTTPS 동일 출처 경로에서 작성했습니다.",
      visibilityType: "NICKNAME",
      failureSize: "SMALL",
      emotionTag: "검증",
      advicePreference: "COMFORT",
      retryIntention: true,
      nextAttemptPlan: "다음 배포에서도 같은 스모크 테스트를 실행합니다.",
    },
  });
  assertStatus(created, [200, 201], "게시글 작성");
  const createdBody = parseJson(created, "게시글 작성");
  assert(createdBody.postId, "작성된 게시글 ID가 없습니다.");

  const detail = await request({ path: `/api/v1/posts/${createdBody.postId}` });
  assertStatus(detail, 200, "게시글 공개 조회");
  assert(parseJson(detail, "게시글 공개 조회").title === title, "작성한 게시글 제목이 다릅니다.");
  logStep(8, "인증 게시글 작성과 공개 조회");

  const logout = await request({
    path: "/api/v1/auth/logout",
    method: "POST",
    headers: { Cookie: rotatedCookie },
  });
  assertStatus(logout, 204, "로그아웃");

  const refreshAfterLogout = await request({
    path: "/api/v1/auth/refresh",
    method: "POST",
    headers: { Cookie: rotatedCookie },
  });
  assertStatus(refreshAfterLogout, 401, "로그아웃 후 토큰 갱신 차단");
  logStep(9, "로그아웃 후 Refresh Token 폐기");

  logStep(10, "운영 HTTPS 핵심 흐름 전체 통과");
}

run()
  .catch((error) => {
    process.stderr.write(`운영 배포 스모크 실패: ${error.message}\n`);
    process.exitCode = 1;
  })
  .finally(() => httpsAgent.destroy());
