"use client";

import Link from "next/link";
import type { AdminReport } from "@/lib/api/admin";
import styles from "@/app/admin/page.module.css";

type ReportListProps = {
  reports: AdminReport[];
  status: string;
  onStatusChange: (status: string) => void;
  onModerate: (postId: number, action: "hide" | "unhide") => Promise<void>;
};

export function ReportList({
  reports,
  status,
  onStatusChange,
  onModerate,
}: ReportListProps) {
  return (
    <section className={styles.reports}>
      <div>
        <h2>신고 목록</h2>
        <select aria-label="신고 상태" value={status} onChange={(event) => onStatusChange(event.target.value)}>
          <option value="PENDING">처리 대기</option>
          <option value="RESOLVED">처리 완료</option>
          <option value="REJECTED">반려</option>
        </select>
      </div>
      {reports.length === 0 ? (
        <p>해당 상태의 신고가 없습니다.</p>
      ) : reports.map((report) => (
        <article key={report.reportId} data-testid={`report-${report.reportId}`}>
          <div>
            <strong>게시글 #{report.targetId}</strong>
            <span>{report.reasonType} · 신고자 #{report.reporterUserId}</span>
          </div>
          <p>{report.reasonDetail || "상세 내용 없음"}</p>
          <div>
            <Link href={`/posts/${report.targetId}`}>원문 확인</Link>
            <button type="button" onClick={() => void onModerate(report.targetId, "hide")}>숨김</button>
            <button type="button" onClick={() => void onModerate(report.targetId, "unhide")}>복구</button>
          </div>
        </article>
      ))}
    </section>
  );
}
