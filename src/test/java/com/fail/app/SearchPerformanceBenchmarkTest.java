package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("performance")
class SearchPerformanceBenchmarkTest extends MySqlContainerIntegrationSupport {

    private static final int WARMUP_COUNT = 3;
    private static final int SAMPLE_COUNT = 10;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void compareLikeAndFullTextWithReproducibleData() throws IOException {
        int postCount = Integer.getInteger("benchmark.search.post-count", 10_000);
        BenchmarkFixture fixture = createFixture(postCount);
        List<SearchScenario> scenarios = List.of(
                new SearchScenario("제목 검색", "다이어트", null),
                new SearchScenario("본문 검색", "알고리즘", null),
                new SearchScenario("카테고리 결합 검색", "면접", fixture.categoryB())
        );

        StringBuilder report = new StringBuilder()
                .append("# Re:Fail 검색 벤치마크\n\n")
                .append("- MySQL: ").append(MYSQL.getDockerImageName()).append('\n')
                .append("- 게시글: ").append(postCount).append("건\n")
                .append("- 워밍업: ").append(WARMUP_COUNT).append("회\n")
                .append("- 측정: ").append(SAMPLE_COUNT).append("회\n\n")
                .append("| 시나리오 | 방식 | 결과 수 | 평균(ms) | p95(ms) |\n")
                .append("| --- | --- | ---: | ---: | ---: |\n");

        for (SearchScenario scenario : scenarios) {
            Measurement like = measureLike(scenario);
            Measurement fullText = measureFullText(scenario);
            assertThat(fullText.resultCount()).isEqualTo(like.resultCount());
            appendMeasurement(report, scenario.name(), "LIKE", like);
            appendMeasurement(report, scenario.name(), "FULLTEXT", fullText);
            report.append("\n<details><summary>")
                    .append(scenario.name())
                    .append(" 실행 계획</summary>\n\n```text\nLIKE: ")
                    .append(explainLike(scenario))
                    .append("\nFULLTEXT: ")
                    .append(explainFullText(scenario))
                    .append("\n```\n</details>\n\n");
        }

        Path reportPath = Path.of("build", "reports", "search-benchmark-" + postCount + ".md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(
                reportPath,
                report.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        System.out.println("Search benchmark report: " + reportPath.toAbsolutePath());
    }

    private BenchmarkFixture createFixture(int postCount) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                insert into users (
                    email, password_hash, nickname, role, status,
                    created_at, updated_at
                ) values (?, ?, ?, 'USER', 'ACTIVE', ?, ?)
                """, "benchmark@refail.local", "hash", "검색벤치마크", now, now);
        Long userId = jdbcTemplate.queryForObject(
                "select id from users where email = 'benchmark@refail.local'",
                Long.class
        );
        jdbcTemplate.update("""
                insert into categories (name, slug, display_order, is_active, created_at, updated_at)
                values ('벤치마크A', 'benchmark-a', 900, true, ?, ?),
                       ('벤치마크B', 'benchmark-b', 901, true, ?, ?)
                """, now, now, now, now);
        Long categoryA = jdbcTemplate.queryForObject(
                "select id from categories where slug = 'benchmark-a'",
                Long.class
        );
        Long categoryB = jdbcTemplate.queryForObject(
                "select id from categories where slug = 'benchmark-b'",
                Long.class
        );

        String sql = """
                insert into posts (
                    user_id, category_id, title, content, visibility_type,
                    failure_size, emotion_tag, advice_preference, retry_intention,
                    reaction_count, report_count, hidden, created_at, updated_at
                ) values (?, ?, ?, ?, 'NICKNAME', 'SMALL', ?, 'ADVICE_OK', true, 0, 0, false, ?, ?)
                """;
        List<Object[]> batch = new ArrayList<>(1000);
        for (int index = 0; index < postCount; index++) {
            Long categoryId = index % 2 == 0 ? categoryA : categoryB;
            String title = index % 10 == 0
                    ? "다이어트 실패를 돌아본 기록 " + index
                    : index % 20 == 1 ? "취업 면접에서 배운 점 " + index : "평범한 일상 기록 " + index;
            String content = index % 25 == 0
                    ? "알고리즘 문제 풀이에서 막힌 원인과 다음 시도를 기록한다."
                    : index % 20 == 1 ? "면접 준비 과정에서 놓친 부분을 다시 정리한다." : "작은 실패를 기록한다.";
            batch.add(new Object[]{
                    userId, categoryId, title, content, index % 10 == 0 ? "아쉬움" : "담담함",
                    Timestamp.valueOf(now.plusNanos(index)), Timestamp.valueOf(now.plusNanos(index))
            });
            if (batch.size() == 1000) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
        jdbcTemplate.execute("analyze table posts");
        return new BenchmarkFixture(categoryA, categoryB);
    }

    private Measurement measureLike(SearchScenario scenario) {
        String sql = likeSql(false);
        Object[] args = likeArgs(scenario);
        return measure(sql, args);
    }

    private Measurement measureFullText(SearchScenario scenario) {
        String sql = fullTextSql(false);
        Object[] args = fullTextArgs(scenario);
        return measure(sql, args);
    }

    private Measurement measure(String sql, Object[] args) {
        for (int index = 0; index < WARMUP_COUNT; index++) {
            jdbcTemplate.queryForObject(sql, Long.class, args);
        }
        List<Double> elapsed = new ArrayList<>(SAMPLE_COUNT);
        long resultCount = 0;
        for (int index = 0; index < SAMPLE_COUNT; index++) {
            long started = System.nanoTime();
            resultCount = jdbcTemplate.queryForObject(sql, Long.class, args);
            elapsed.add((System.nanoTime() - started) / 1_000_000.0);
        }
        elapsed.sort(Comparator.naturalOrder());
        double average = elapsed.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double p95 = elapsed.get((int) Math.ceil(SAMPLE_COUNT * 0.95) - 1);
        return new Measurement(resultCount, average, p95);
    }

    private String explainLike(SearchScenario scenario) {
        return explain(likeSql(true), likeArgs(scenario));
    }

    private String explainFullText(SearchScenario scenario) {
        return explain(fullTextSql(true), fullTextArgs(scenario));
    }

    private String explain(String sql, Object[] args) {
        return String.join(" ", jdbcTemplate.queryForList(sql, String.class, args));
    }

    private String likeSql(boolean explain) {
        return (explain ? "explain analyze " : "") + """
                select count(*)
                  from posts post
                 where post.hidden = false
                   and post.deleted_at is null
                   and (? is null or post.category_id = ?)
                   and (
                       post.title like ?
                       or post.content like ?
                       or post.emotion_tag like ?
                   )
                """;
    }

    private String fullTextSql(boolean explain) {
        return (explain ? "explain analyze " : "") + """
                select count(*)
                  from posts post
                 where post.hidden = false
                   and post.deleted_at is null
                   and (? is null or post.category_id = ?)
                   and match(post.title, post.content, post.emotion_tag) against (?)
                """;
    }

    private Object[] likeArgs(SearchScenario scenario) {
        String pattern = "%" + scenario.keyword() + "%";
        return new Object[]{scenario.categoryId(), scenario.categoryId(), pattern, pattern, pattern};
    }

    private Object[] fullTextArgs(SearchScenario scenario) {
        return new Object[]{scenario.categoryId(), scenario.categoryId(), scenario.keyword()};
    }

    private void appendMeasurement(
            StringBuilder report,
            String scenario,
            String method,
            Measurement measurement
    ) {
        report.append("| ").append(scenario)
                .append(" | ").append(method)
                .append(" | ").append(measurement.resultCount())
                .append(" | ").append("%.2f".formatted(measurement.averageMs()))
                .append(" | ").append("%.2f".formatted(measurement.p95Ms()))
                .append(" |\n");
    }

    private record BenchmarkFixture(Long categoryA, Long categoryB) {
    }

    private record SearchScenario(String name, String keyword, Long categoryId) {
    }

    private record Measurement(long resultCount, double averageMs, double p95Ms) {
    }
}
