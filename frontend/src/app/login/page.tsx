import { AuthShell } from "@/components/auth/AuthShell";
import { LoginForm } from "@/components/auth/LoginForm";

type LoginPageProps = {
  searchParams: Promise<{ email?: string; joined?: string }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const params = await searchParams;
  return (
    <AuthShell
      index="ACCOUNT · 01"
      title={<>실패 뒤의 이야기를<br />계속 기록하세요.</>}
      description="지난 실패를 돌아보고, 달라진 생각과 새로운 시도를 이어서 남길 수 있습니다."
    >
      <LoginForm initialEmail={params.email} joined={params.joined === "true"} />
    </AuthShell>
  );
}
