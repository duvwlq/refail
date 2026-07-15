package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fail.app.common.security.JwtTokenProvider;
import com.fail.app.domain.moderation.entity.ModerationActionType;
import com.fail.app.domain.moderation.repository.ModerationActionRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUserModerationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModerationActionRepository moderationActionRepository;

    @Test
    void userRestrictionAndActivationAreRecordedAsUserTargets() throws Exception {
        User admin = saveUser("moderation-admin@example.com", "moderation-admin", UserRole.ADMIN);
        User target = saveUser("moderation-target@example.com", "moderation-target", UserRole.USER);
        String adminToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/restrict", target.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"repeated policy violation\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/activate", target.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        var actions = moderationActionRepository.findAll();
        assertThat(actions).hasSize(2);
        assertThat(actions)
                .allSatisfy(action -> {
                    assertThat(action.getTargetType().name()).isEqualTo("USER");
                    assertThat(action.getTargetId()).isEqualTo(target.getId());
                });
        assertThat(actions)
                .extracting(action -> action.getActionType())
                .containsExactlyInAnyOrder(
                        ModerationActionType.RESTRICT_USER,
                        ModerationActionType.ACTIVATE_USER
                );
    }

    private User saveUser(String email, String nickname, UserRole role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("hash")
                .nickname(nickname)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
