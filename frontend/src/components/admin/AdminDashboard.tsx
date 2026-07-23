"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { CategoryManager } from "@/components/admin/CategoryManager";
import { MetricsPanel } from "@/components/admin/MetricsPanel";
import { ReportList } from "@/components/admin/ReportList";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { ApiError } from "@/lib/api";
import {
  deactivateCategory,
  getOperationMetrics,
  getReports,
  moderatePost,
  saveCategory,
  type AdminReport,
  type CategoryPayload,
  type OperationMetrics,
} from "@/lib/api/admin";
import { getCategories } from "@/lib/api/categories";
import type { Category } from "@/types/post";
import styles from "@/app/admin/page.module.css";

export function AdminDashboard() {
  const auth = useRequireAuth("ADMIN");
  const [reports, setReports] = useState<AdminReport[]>([]);
  const [metrics, setMetrics] = useState<OperationMetrics | null>(null);
  const [status, setStatus] = useState("PENDING");
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState("");

  async function load(selectedStatus: string, token: string) {
    try {
      const { reportPage, operationMetrics, categoryItems } = await fetchDashboard(
        selectedStatus,
        token,
      );
      setReports(reportPage.content);
      setMetrics(operationMetrics);
      setCategories(categoryItems);
      setError("");
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "관리자 데이터를 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    if (!auth.ready || !auth.token) return;
    void fetchDashboard(status, auth.token)
      .then(({ reportPage, operationMetrics, categoryItems }) => {
        setReports(reportPage.content);
        setMetrics(operationMetrics);
        setCategories(categoryItems);
        setError("");
      })
      .catch((caught) => {
        setError(caught instanceof ApiError ? caught.message : "관리자 데이터를 불러오지 못했습니다.");
      });
  }, [auth.ready, auth.token, status]);

  async function handleModeration(postId: number, action: "hide" | "unhide") {
    if (!auth.token) return;
    try {
      await moderatePost(postId, action, auth.token);
      await load(status, auth.token);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "게시글 상태를 변경하지 못했습니다.");
    }
  }

  async function handleSaveCategory(categoryId: number | null, payload: CategoryPayload) {
    if (!auth.token) return;
    try {
      await saveCategory(categoryId, payload, auth.token);
      await load(status, auth.token);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "카테고리를 저장하지 못했습니다.");
      throw caught;
    }
  }

  async function handleDeactivateCategory(categoryId: number) {
    if (!auth.token) return;
    try {
      await deactivateCategory(categoryId, auth.token);
      await load(status, auth.token);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "카테고리를 비활성화하지 못했습니다.");
    }
  }

  if (!auth.ready) {
    return <main className={styles.page}>관리자 권한을 확인하고 있습니다.</main>;
  }

  return (
    <main className={styles.page}>
      <header>
        <Link href="/" className={styles.brand}>Re:<span>Fail</span></Link>
        <Link href="/">서비스로 돌아가기</Link>
      </header>
      <section className={styles.title}>
        <span>OPERATION DESK</span>
        <h1>운영 대시보드</h1>
      </section>
      {error && <p role="alert">{error}</p>}
      {metrics && <MetricsPanel metrics={metrics} />}
      <CategoryManager
        categories={categories}
        onSave={handleSaveCategory}
        onDeactivate={handleDeactivateCategory}
      />
      <ReportList
        reports={reports}
        status={status}
        onStatusChange={setStatus}
        onModerate={handleModeration}
      />
    </main>
  );
}

async function fetchDashboard(status: string, token: string) {
  const [reportPage, operationMetrics, categoryItems] = await Promise.all([
    getReports(status, token),
    getOperationMetrics(token),
    getCategories(),
  ]);
  return { reportPage, operationMetrics, categoryItems };
}
