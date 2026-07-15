import { notFound } from "next/navigation";
import { PostDetailView } from "@/components/post/PostDetailView";
import { apiFetch } from "@/lib/api";
import type { PostDetail } from "@/types/post";

async function getPost(postId: string): Promise<PostDetail | null> {
  try {
    return await apiFetch<PostDetail>(`/api/v1/posts/${postId}`, { cache: "no-store" });
  } catch {
    return null;
  }
}

export default async function PostDetailPage({ params }: { params: Promise<{ postId: string }> }) {
  const { postId } = await params;
  if (!/^\d+$/.test(postId)) notFound();
  const post = await getPost(postId);
  if (!post) notFound();
  return <PostDetailView post={post} />;
}
