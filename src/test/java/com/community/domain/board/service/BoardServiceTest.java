package com.community.domain.board.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.dto.response.GetAllBoardResponse;
import com.community.domain.board.entity.Board;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.board.repository.BoardRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @InjectMocks
    private BoardService boardService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;


    // ========== 게시판 목록 조회 ==========
    @Test
    @DisplayName("게시판 목록 조회 성공")
    void getAll_success() {
        // given
        Board freeBoard = Board.register("자유게시판", null);
        ReflectionTestUtils.setField(freeBoard, "id", 1L);

        Board questionBoard = Board.register("질문게시판", null);
        ReflectionTestUtils.setField(questionBoard, "id", 2L);

        Board noticeBoard = Board.register("공지사항", null);
        ReflectionTestUtils.setField(noticeBoard, "id", 3L);

        given(boardRepository.findAll()).willReturn(List.of(freeBoard, questionBoard, noticeBoard));

        // when
        List<GetAllBoardResponse> response = boardService.getAll();

        // then
        assertThat(response).hasSize(3);

        assertThat(response.getFirst().id()).isEqualTo(1L);
        assertThat(response.getFirst().name()).isEqualTo("자유게시판");

        assertThat(response.get(1).id()).isEqualTo(2L);
        assertThat(response.get(1).name()).isEqualTo("질문게시판");

        assertThat(response.getLast().id()).isEqualTo(3L);
        assertThat(response.getLast().name()).isEqualTo("공지사항");
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 게시판 없음")
    void getAll_success_noBoard() {
        // given
        given(boardRepository.findAll()).willReturn(List.of());

        // when
        List<GetAllBoardResponse> response = boardService.getAll();

        // then
        assertThat(response).isEmpty();
    }


    // ========== 게시판 접근 권한 검증 ==========
    @Test
    @DisplayName("게시판 접근 권한 검증 성공")
    void validateBoardAccess_success() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Board board = Board.register("자유게시판", UserRole.BRONZE);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));

        // when & then
        assertDoesNotThrow(() -> boardService.validateBoardAccess(user.getId(), board.getId()));
    }

    @Test
    @DisplayName("게시판 접근 권한 검증 성공 - 공개게시판")
    void validateBoardAccess_success_minRoleIsNull() {
        // given
        Long userId = 1L;

        Board board = Board.register("공개게시판", null);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));

        // when & then
        assertDoesNotThrow(() -> boardService.validateBoardAccess(userId, board.getId()));
    }

    @Test
    @DisplayName("게시판 접근 권한 검증 실패 - 게시판 없음")
    void validateBoardAccess_fail_boardNotFound() {
        // given
        Long userId = 1L;
        Long boardId = 99L;

        given(boardRepository.findById(boardId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.validateBoardAccess(userId, boardId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시판 접근 권한 검증 실패 - 미인증 사용자")
    void validateBoardAccess_fail_unauthorized() {
        // given
        Long userId = null;

        Board board = Board.register("자유게시판", UserRole.BRONZE);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));

        // when & then
        assertThatThrownBy(() -> boardService.validateBoardAccess(userId, board.getId()))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("게시판 접근 권한 검증 실패 - 삭제된 사용자")
    void validateBoardAccess_fail_deletedUser() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "deletedAt", LocalDateTime.of(2026, 1, 1, 0, 0, 0));

        Board board = Board.register("자유게시판", UserRole.BRONZE);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.validateBoardAccess(user.getId(), board.getId()))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시판 접근 권한 검증 실패 - 접근 권한 없음")
    void validateBoardAccess_fail_forbidden() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        Board board = Board.register("운영게시판", UserRole.MANAGER);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> boardService.validateBoardAccess(user.getId(), board.getId()))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_ACCESS_DENIED.getMessage());
    }
}