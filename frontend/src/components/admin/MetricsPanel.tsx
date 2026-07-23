import type { OperationMetrics } from "@/lib/api/admin";
import styles from "@/app/admin/page.module.css";

export function MetricsPanel({ metrics }: { metrics: OperationMetrics }) {
  return (
    <section className={styles.metrics}>
      <article><span>전체 기록</span><strong>{metrics.totalPosts}</strong></article>
      <article><span>후속 기록률</span><strong>{metrics.updateRate}%</strong></article>
      <article><span>대기 신고</span><strong>{metrics.pendingReports}</strong></article>
      <article><span>극복 기록</span><strong>{metrics.succeededUpdates}</strong></article>
    </section>
  );
}
