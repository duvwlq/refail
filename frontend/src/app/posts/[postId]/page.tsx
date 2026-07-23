import { notFound } from "next/navigation";
import { PostDetailView } from "@/components/post/PostDetailView";
import { getPost } from "@/lib/api/posts";
import type { PostDetail } from "@/types/post";

async function loadPost(postId: string): Promise<PostDetail | null> {
  try {
    return await getPost(Number(postId), { cache: "no-store" });
  } catch {
    return null;
  }
}

export default async function PostDetailPage({ params }: { params: Promise<{ postId: string }> }) {
  const { postId } = await params;
  if (!/^\d+$/.test(postId)) notFound();
  const post = await loadPost(postId);
  if (!post) notFound();
  return <PostDetailView post={post} />;
}
