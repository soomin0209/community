package com.community.domain.reaction.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.domain.reaction.dto.request.ReactionRequest;
import com.community.domain.reaction.dto.response.ReactionResponse;
import com.community.domain.reaction.enums.ReactionType;
import com.community.domain.reaction.service.ReactionService;
import com.community.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReactionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ReactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReactionService reactionService;


    // =========== 좋아요/싫어요 ==========
    @Test
    @DisplayName("좋아요/싫어요 성공")
    void react_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        ReactionRequest request = new ReactionRequest(ReactionType.LIKE);
        ReactionResponse response = new ReactionResponse(1L, 1L, 1L, ReactionType.LIKE);

        given(reactionService.react(eq(1L), eq(1L), any(ReactionRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/reactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.postId").value(1L))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.type").value(ReactionType.LIKE.name()));
    }

    @Test
    @DisplayName("좋아요/싫어요 실패 - 타입 공백")
    void react_fail_typeIsNull() throws Exception {
        // given
        ReactionRequest request = new ReactionRequest(null);

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/reactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("좋아요/싫어요 선택해주세요"));
    }
}