"use client";

import { useState } from "react";
import type { CategoryPayload } from "@/lib/api/admin";
import type { Category } from "@/types/post";
import styles from "@/app/admin/page.module.css";

type CategoryManagerProps = {
  categories: Category[];
  onSave: (categoryId: number | null, payload: CategoryPayload) => Promise<void>;
  onDeactivate: (categoryId: number) => Promise<void>;
};

export function CategoryManager({
  categories,
  onSave,
  onDeactivate,
}: CategoryManagerProps) {
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [displayOrder, setDisplayOrder] = useState(0);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    try {
      await onSave(editingId, { name, slug, displayOrder });
      setName("");
      setSlug("");
      setDisplayOrder(0);
      setEditingId(null);
    } catch {
      // The dashboard owns and displays API errors.
    } finally {
      setPending(false);
    }
  }

  function startEditing(category: Category) {
    setEditingId(category.categoryId);
    setName(category.name);
    setSlug(category.slug);
    setDisplayOrder(category.displayOrder);
  }

  return (
    <section className={styles.categories}>
      <h2>카테고리 관리</h2>
      <form onSubmit={handleSubmit}>
        <input required value={name} onChange={(event) => setName(event.target.value)} placeholder="카테고리 이름" />
        <input required value={slug} onChange={(event) => setSlug(event.target.value)} placeholder="영문 slug" />
        <input type="number" min="0" value={displayOrder} onChange={(event) => setDisplayOrder(Number(event.target.value))} />
        <button disabled={pending}>{pending ? "저장 중..." : editingId ? "수정 완료" : "추가"}</button>
      </form>
      <div>
        {categories.map((category) => (
          <article key={category.categoryId}>
            <span>{category.name} · {category.slug}</span>
            <button type="button" onClick={() => startEditing(category)}>수정</button>
            <button type="button" onClick={() => void onDeactivate(category.categoryId)}>비활성화</button>
          </article>
        ))}
      </div>
    </section>
  );
}
