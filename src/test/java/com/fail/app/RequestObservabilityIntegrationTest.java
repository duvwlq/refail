package com.fail.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fail.app.common.security.JwtTokenProvider;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RequestObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void everyResponseHasRequestIdAndValidUpstreamIdIsPreserved() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"));

        mockMvc.perform(get("/api/v1/health").header("X-Request-ID", "trace-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "trace-123"));
    }

    @Test
    void actuatorExposesHealthLatencyCountsAndPrometheusMetrics() throws Exception {
        User admin = userRepository.save(User.builder()
                .email("metrics-admin@example.com")
                .passwordHash("hash")
                .nickname("metrics-admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        String authorization = "Bearer " + jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/metrics/http.server.requests")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[?(@.statistic == 'COUNT')].value").exists())
                .andExpect(jsonPath("$.measurements[?(@.statistic == 'TOTAL_TIME')].value").exists());
        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("http_server_requests_seconds")));
    }
}
