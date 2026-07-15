package com.fail.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiDocumentationHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 헬스_체크에서_애플리케이션과_DB_상태를_확인한다() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.checkedAt").exists());
    }

    @Test
    void OpenAPI_문서에_JWT와_주요_API가_포함된다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("실패 공유 서비스 API"))
                .andExpect(jsonPath("$.components.securitySchemes['Bearer JWT'].type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes['Bearer JWT'].scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/health'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/reports'].get.security").isArray());
    }

    @Test
    void 프론트엔드_출처의_CORS_사전요청을_허용한다() throws Exception {
        mockMvc.perform(options("/api/v1/posts")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
