package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
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
class PostMutationAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    void 작성자_JWT만_게시글을_수정하고_삭제할_수_있다() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("수정 테스트")
                .slug("mutation-test")
                .displayOrder(10)
                .isActive(true)
                .build());
        String authorToken = signupAndLogin("author@example.com", "작성자");
        String otherToken = signupAndLogin("other@example.com", "다른사용자");
        Long postId = createPost(authorToken, category.getId());
        Long updateId = createPostUpdate(authorToken, postId);
        String updateBody = updateBody(category.getId());

        mockMvc.perform(get("/api/v1/posts/{postId}/ownership", postId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownedByMe").value(true));

        mockMvc.perform(get("/api/v1/posts/{postId}/ownership", postId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownedByMe").value(false));

        mockMvc.perform(patch("/api/v1/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        mockMvc.perform(patch("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));

        mockMvc.perform(patch("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 실패 기록"))
                .andExpect(jsonPath("$.failureSize").value("MEDIUM"))
                .andExpect(jsonPath("$.retryIntention").value(false));

        String postUpdateBody = """
                {"status":"IMPROVING","content":"조금씩 나아지고 있다."}
                """;
        mockMvc.perform(patch("/api/v1/posts/{postId}/updates/{updateId}", postId, updateId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postUpdateBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));

        mockMvc.perform(patch("/api/v1/posts/{postId}/updates/{updateId}", postId, updateId)
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postUpdateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IMPROVING"))
                .andExpect(jsonPath("$.content").value("조금씩 나아지고 있다."));

        mockMvc.perform(delete("/api/v1/posts/{postId}/updates/{updateId}", postId, updateId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));

        mockMvc.perform(delete("/api/v1/posts/{postId}/updates/{updateId}", postId, updateId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updateId").value(updateId))
                .andExpect(jsonPath("$.deleted").value(true));

        mockMvc.perform(get("/api/v1/posts/{postId}/updates", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        createPostUpdate(authorToken, postId);

        mockMvc.perform(delete("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));

        mockMvc.perform(delete("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.deleted").value(true));

        assertThat(postRepository.findById(postId).orElseThrow().isDeleted()).isTrue();
        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"));
        mockMvc.perform(get("/api/v1/posts/{postId}/updates", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"));
    }

    private String signupAndLogin(String email, String nickname) throws Exception {
        String signupBody = """
                {"email":"%s","password":"password123","nickname":"%s"}
                """.formatted(email, nickname);
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

    private Long createPost(String token, Long categoryId) throws Exception {
        String body = """
                {
                  "categoryId": %d,
                  "title": "수정 전 실패 기록",
                  "content": "수정 전 내용",
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

    private Long createPostUpdate(String token, Long postId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/posts/{postId}/updates", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TRYING_AGAIN\",\"content\":\"다시 시도한다.\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("updateId").asLong();
    }

    private String updateBody(Long categoryId) {
        return """
                {
                  "categoryId": %d,
                  "title": "수정된 실패 기록",
                  "content": "실패 원인을 다시 정리했다.",
                  "failureSize": "MEDIUM",
                  "emotionTag": "차분함",
                  "advicePreference": "COMFORT",
                  "retryIntention": false
                }
                """.formatted(categoryId);
    }
}
