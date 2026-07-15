import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import styles from "./MarkdownContent.module.css";

export function MarkdownContent({ content }: { content: string }) {
  return (
    <div className={styles.markdown}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          a: ({ children, ...props }) => <a {...props} target="_blank" rel="noreferrer">{children}</a>,
          img: ({ alt }) => <span className={styles.imageNotice}>이미지는 현재 지원하지 않습니다: {alt || "설명 없음"}</span>,
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
