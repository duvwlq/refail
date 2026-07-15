import { AuthShell } from "@/components/auth/AuthShell";
import { SignupForm } from "@/components/auth/SignupForm";

export default function SignupPage() {
  return (
    <AuthShell
      index="ACCOUNT · 00"
      title={<>완벽하지 않은<br />기록을 시작하세요.</>}
      description="실명은 필요하지 않습니다. 닉네임을 만들고, 글을 남길 때마다 익명 공개 여부를 선택하세요."
    >
      <SignupForm />
    </AuthShell>
  );
}
