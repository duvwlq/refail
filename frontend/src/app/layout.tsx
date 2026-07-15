import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Re:Fail | 실패를 공유하고, 다음을 준비하다",
  description: "크고 작은 실패를 공유하고 다음을 준비하는 공간",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
