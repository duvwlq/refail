package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReactionReportFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    void jwt로_공감_변경_취소와_신고_중복방지를_처리한다() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("일상")
                .slug("daily")
                .displayOrder(1)
                .isActive(true)
                .build());

        String authorToken = signupAndLogin("flow-author@example.com", "작성자테스터");
        String token = signupAndLogin("flow-reactor@example.com", "반응테스터");
        Long postId = createPost(authorToken, category.getId());

        mockMvc.perform(put("/api/v1/posts/{postId}/reaction", postId)
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactionType\":\"ME_TOO\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/posts/{postId}/reaction", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactionType\":\"ME_TOO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactionType").value("ME_TOO"))
                .andExpect(jsonPath("$.applied").value(true));

        mockMvc.perform(get("/api/v1/posts/{postId}/reaction", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactionType").value("ME_TOO"))
                .andExpect(jsonPath("$.applied").value(true));

        mockMvc.perform(put("/api/v1/posts/{postId}/reaction", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactionType\":\"SEND_SUPPORT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactionType").value("SEND_SUPPORT"));

        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactionCount").value(1));

        mockMvc.perform(delete("/api/v1/posts/{postId}/reaction", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(false));

        mockMvc.perform(get("/api/v1/posts/{postId}/reaction", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactionType").doesNotExist())
                .andExpect(jsonPath("$.applied").value(false));

        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactionCount").value(0));

        String reportBody = "{\"reasonType\":\"SPAM\",\"reasonDetail\":\"반복 광고\"}";
        mockMvc.perform(post("/api/v1/posts/{postId}/reports", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(postId))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/v1/posts/{postId}/reports", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_001"));

        Post savedPost = postRepository.findById(postId).orElseThrow();
        assertThat(savedPost.getReportCount()).isEqualTo(1);
    }

    private String signupAndLogin(String email, String nickname) throws Exception {
        String credentials = "{\"email\":\"" + email + "\",\"password\":\"password123\",\"nickname\":\"" + nickname + "\"}";
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private Long createPost(String token, Long categoryId) throws Exception {
        String body = """
                {
                  "categoryId": %d,
                  "title": "작은 실패 기록",
                  "content": "계획한 일을 끝내지 못했다.",
                  "visibilityType": "NICKNAME",
                  "failureSize": "SMALL",
                  "emotionTag": "아쉬움",
                  "advicePreference": "ADVICE_OK",
                  "retryIntention": true
                }
                """.formatted(categoryId);

        MvcResult result = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("postId").asLong();
    }
}
