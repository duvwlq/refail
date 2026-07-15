"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { getAccessToken } from "@/lib/auth";
import type { AuthUser } from "@/types/auth";
import type { Category, PageResponse } from "@/types/post";
import styles from "./page.module.css";

type Report={reportId:number;targetId:number;reasonType:string;reasonDetail:string|null;status:string;reporterUserId:number};
type Metrics={totalPosts:number;updateRate:number;pendingReports:number;succeededUpdates:number};

export default function AdminPage(){
 const router=useRouter();const[reports,setReports]=useState<Report[]>([]);const[metrics,setMetrics]=useState<Metrics|null>(null);const[status,setStatus]=useState("PENDING");const[categories,setCategories]=useState<Category[]>([]);const[name,setName]=useState("");const[slug,setSlug]=useState("");const[order,setOrder]=useState(0);const[editing,setEditing]=useState<number|null>(null);
 async function load(selected=status){const token=getAccessToken();if(!token)return router.replace("/login");const me=await apiFetch<AuthUser>("/api/v1/auth/me",{token});if(me.role!=="ADMIN")return router.replace("/");const[r,m,c]=await Promise.all([apiFetch<PageResponse<Report>>(`/api/v1/admin/reports?status=${selected}&page=0&size=50`,{token}),apiFetch<Metrics>("/api/v1/admin/metrics",{token}),apiFetch<Category[]>("/api/v1/categories")]);setReports(r.content);setMetrics(m);setCategories(c)}
 // eslint-disable-next-line react-hooks/set-state-in-effect, react-hooks/exhaustive-deps
 useEffect(()=>{void load()},[]);
 async function moderate(id:number,action:"hide"|"unhide"){const token=getAccessToken();if(!token)return;await apiFetch(`/api/v1/admin/posts/${id}/${action}`,{method:"PATCH",token,body:action==="hide"?JSON.stringify({reason:"신고 검토 후 숨김"}):undefined});await load(status)}
 async function saveCategory(e:React.FormEvent){e.preventDefault();const token=getAccessToken();if(!token)return;await apiFetch(editing?`/api/v1/admin/categories/${editing}`:"/api/v1/admin/categories",{method:editing?"PATCH":"POST",token,body:JSON.stringify({name,slug,displayOrder:order})});setName("");setSlug("");setEditing(null);await load(status)}
 async function deactivate(id:number){const token=getAccessToken();if(!token)return;await apiFetch(`/api/v1/admin/categories/${id}`,{method:"DELETE",token});await load(status)}
 return <main className={styles.page}><header><Link href="/" className={styles.brand}>Re:<span>Fail</span></Link><Link href="/">서비스로 돌아가기</Link></header><section className={styles.title}><span>OPERATION DESK</span><h1>운영 대시보드</h1></section>
 {metrics&&<section className={styles.metrics}><article><span>전체 기록</span><strong>{metrics.totalPosts}</strong></article><article><span>후속 기록률</span><strong>{metrics.updateRate}%</strong></article><article><span>대기 신고</span><strong>{metrics.pendingReports}</strong></article><article><span>극복 기록</span><strong>{metrics.succeededUpdates}</strong></article></section>}
 <section className={styles.categories}><h2>카테고리 관리</h2><form onSubmit={saveCategory}><input required value={name} onChange={e=>setName(e.target.value)} placeholder="카테고리 이름"/><input required value={slug} onChange={e=>setSlug(e.target.value)} placeholder="영문 slug"/><input type="number" min="0" value={order} onChange={e=>setOrder(Number(e.target.value))}/><button>{editing?"수정 완료":"추가"}</button></form><div>{categories.map(c=><article key={c.categoryId}><span>{c.name} · {c.slug}</span><button onClick={()=>{setEditing(c.categoryId);setName(c.name);setSlug(c.slug);setOrder(c.displayOrder)}}>수정</button><button onClick={()=>deactivate(c.categoryId)}>비활성화</button></article>)}</div></section>
 <section className={styles.reports}><div><h2>신고 목록</h2><select value={status} onChange={e=>{setStatus(e.target.value);void load(e.target.value)}}><option value="PENDING">처리 대기</option><option value="RESOLVED">처리 완료</option><option value="REJECTED">반려</option></select></div>{reports.length===0?<p>해당 상태의 신고가 없습니다.</p>:reports.map(r=><article key={r.reportId}><div><strong>게시글 #{r.targetId}</strong><span>{r.reasonType} · 신고자 #{r.reporterUserId}</span></div><p>{r.reasonDetail||"상세 내용 없음"}</p><div><Link href={`/posts/${r.targetId}`}>원문 확인</Link><button onClick={()=>moderate(r.targetId,"hide")}>숨김</button><button onClick={()=>moderate(r.targetId,"unhide")}>복구</button></div></article>)}</section></main>
}
