"use client";

import { useRef } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import styles from "./MarkdownEditor.module.css";

type MarkdownEditorProps = {
  value: string;
  onChange: (value: string) => void;
};

type Tool = {
  label: string;
  title: string;
  before: string;
  after?: string;
  placeholder: string;
  lineStart?: boolean;
};

const tools: Tool[] = [
  { label: "H", title: "제목", before: "## ", placeholder: "소제목", lineStart: true },
  { label: "B", title: "굵게", before: "**", after: "**", placeholder: "강조할 내용" },
  { label: "I", title: "기울임", before: "_", after: "_", placeholder: "기울일 내용" },
  { label: "❝", title: "인용", before: "> ", placeholder: "돌아보며 든 생각", lineStart: true },
  { label: "•", title: "목록", before: "- ", placeholder: "목록 항목", lineStart: true },
  { label: "↗", title: "링크", before: "[", after: "](https://)", placeholder: "링크 이름" },
  { label: "</>", title: "코드", before: "`", after: "`", placeholder: "code" },
];

export function MarkdownEditor({ value, onChange }: MarkdownEditorProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  function applyTool(tool: Tool) {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selected = value.slice(start, end) || tool.placeholder;
    const prefix = tool.lineStart && start > 0 && value[start - 1] !== "\n" ? `\n${tool.before}` : tool.before;
    const inserted = `${prefix}${selected}${tool.after ?? ""}`;
    onChange(`${value.slice(0, start)}${inserted}${value.slice(end)}`);

    requestAnimationFrame(() => {
      textarea.focus();
      const selectionStart = start + prefix.length;
      textarea.setSelectionRange(selectionStart, selectionStart + selected.length);
    });
  }

  function insertDivider() {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const divider = start > 0 && value[start - 1] !== "\n" ? "\n\n---\n\n" : "---\n\n";
    onChange(`${value.slice(0, start)}${divider}${value.slice(start)}`);
    requestAnimationFrame(() => textarea.focus());
  }

  return (
    <div className={styles.editor}>
      <div className={styles.toolbar} aria-label="마크다운 도구">
        <span className={styles.mode}>MARKDOWN</span>
        {tools.map((tool) => (
          <button type="button" key={tool.title} title={tool.title} aria-label={tool.title} onClick={() => applyTool(tool)}>
            {tool.label}
          </button>
        ))}
        <button type="button" title="구분선" aria-label="구분선" onClick={insertDivider}>—</button>
        <a href="https://www.markdownguide.org/basic-syntax/" target="_blank" rel="noreferrer">문법 도움말</a>
      </div>
      <div className={styles.panes}>
        <div className={styles.writePane}>
          <label htmlFor="post-content">글쓰기</label>
          <textarea
            ref={textareaRef}
            id="post-content"
            value={value}
            onChange={(event) => onChange(event.target.value)}
            required
            spellCheck
            placeholder={"## 무엇을 하려고 했나요?\n\n시작할 때의 계획을 적어보세요.\n\n## 어디에서 달라졌나요?\n\n- 예상하지 못한 일\n- 다음에는 바꾸고 싶은 점"}
          />
          <span className={styles.count}>{value.length.toLocaleString("ko-KR")}자</span>
        </div>
        <div className={styles.previewPane}>
          <span className={styles.previewLabel}>미리보기</span>
          {value.trim() ? (
            <article className={styles.markdown}>
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                  a: ({ children, ...props }) => <a {...props} target="_blank" rel="noreferrer">{children}</a>,
                  img: ({ alt }) => <span className={styles.imageNotice}>이미지는 현재 지원하지 않습니다: {alt || "설명 없음"}</span>,
                }}
              >
                {value}
              </ReactMarkdown>
            </article>
          ) : (
            <div className={styles.previewEmpty}>
              <strong>작성한 글이 여기에 보여요.</strong>
              <p>제목, 목록, 인용문을 사용해 실패의 과정을 차분히 정리해 보세요.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
