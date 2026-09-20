package com.community.domain.board.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.dto.request.CreateBoardRequest;
import com.community.domain.board.dto.request.UpdateBoardRequest;
import com.community.domain.board.dto.response.CreateBoardResponse;
import com.community.domain.board.dto.response.UpdateBoardResponse;
import com.community.domain.board.entity.Board;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.board.repository.BoardRepository;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardAdminServiceTest {

    @InjectMocks
    private BoardAdminService boardAdminService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private PostRepository postRepository;


    // ========== 게시판 등록 ==========
    @Test
    @DisplayName("게시판 등록 성공")
    void create_success() {
        // given
        CreateBoardRequest request = new CreateBoardRequest("자유게시판", null);
        Board board = Board.register(request.name(), null);

        given(boardRepository.existsByName(request.name())).willReturn(false);
        given(boardRepository.save(any(Board.class))).willReturn(board);

        // when
        CreateBoardResponse response = boardAdminService.create(request);

        // then
        assertThat(response.name()).isEqualTo("자유게시판");
        assertThat(response.minRole()).isNull();
    }

    @Test
    @DisplayName("게시판 등록 실패 - 이름 중복")
    void create_fail_duplicateName() {
        // given
        CreateBoardRequest request = new CreateBoardRequest("자유게시판", null);

        given(boardRepository.existsByName(request.name())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> boardAdminService.create(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.DUPLICATED_NAME.getMessage());
    }


    // ========== 게시판 수정 ==========
    @Test
    @DisplayName("게시판 수정 성공")
    void update_success() {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest("질문게시판", UserRole.BRONZE);
        Board board = Board.register("자유게시판", null);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));
        given(boardRepository.existsByName(request.name())).willReturn(false);

        // when
        UpdateBoardResponse response = boardAdminService.update(board.getId(), request);

        // then
        assertThat(response.name()).isEqualTo("질문게시판");
        assertThat(response.minRole().name()).isEqualTo(UserRole.BRONZE.name());

    }

    @Test
    @DisplayName("게시판 수정 성공 - 이름 수정 안함")
    void update_success_nameIsNull() {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest(null, UserRole.BRONZE);
        Board board = Board.register("자유게시판", null);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));

        // when
        UpdateBoardResponse response = boardAdminService.update(board.getId(), request);

        // then
        assertThat(response.name()).isEqualTo("자유게시판");
        assertThat(response.minRole().name()).isEqualTo(UserRole.BRONZE.name());
    }

    @Test
    @DisplayName("게시판 수정 성공 - 이름이 기존 이름과 같음")
    void update_success_nameIsSame() {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest("자유게시판", UserRole.BRONZE);
        Board board = Board.register("자유게시판", null);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));

        // when
        UpdateBoardResponse response = boardAdminService.update(board.getId(), request);

        // then
        assertThat(response.name()).isEqualTo("자유게시판");
        assertThat(response.minRole().name()).isEqualTo(UserRole.BRONZE.name());
    }

    @Test
    @DisplayName("게시판 수정 성공 - 등급 수정 안함")
    void update_success_minRoleIsNull() {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest("질문게시판", null);
        Board board = Board.register("자유게시판", null);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));

        // when
        UpdateBoardResponse response = boardAdminService.update(board.getId(), request);

        // then
        assertThat(response.name()).isEqualTo("질문게시판");
        assertThat(response.minRole()).isNull();
    }

    @Test
    @DisplayName("게시판 수정 실패 - 게시판 없음")
    void update_fail_boardNotFound() {
        // given
        Long boardId = 99L;
        UpdateBoardRequest request = new UpdateBoardRequest("질문게시판", UserRole.BRONZE);

        given(boardRepository.findById(boardId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardAdminService.update(boardId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시판 수정 실패 - 이름 중복")
    void update_fail_duplicateName() {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest("질문게시판", UserRole.BRONZE);
        Board board = Board.register("자유게시판", null);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));
        given(boardRepository.existsByName(request.name())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> boardAdminService.update(board.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.DUPLICATED_NAME.getMessage());
    }

    @Test
    @DisplayName("게시판 수정 실패 - 수정할 값 없음")
    void update_fail_noUpdateValue() {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest(null, null);
        Board board = Board.register("자유게시판", UserRole.BRONZE);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));

        // when & then
        assertThatThrownBy(() -> boardAdminService.update(board.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_UPDATE_NO_CONTENT.getMessage());
    }


    // ========== 게시판 삭제 ==========
    @Test
    @DisplayName("게시판 삭제 성공")
    void delete_success() {
        // given
        Board board = Board.register("자유게시판", null);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));
        given(postRepository.existsByBoardIdAndDeletedAtIsNull(board.getId())).willReturn(false);

        // when
        boardAdminService.delete(board.getId());

        // then
        verify(boardRepository).delete(board);
    }

    @Test
    @DisplayName("게시판 삭제 실패 - 게시판 없음")
    void delete_fail_boardNotFound() {
        // given
        Long boardId = 99L;

        given(boardRepository.findById(boardId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardAdminService.delete(boardId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시판 삭제 실패 - 하위 게시물 존재")
    void delete_fail_boardInUse() {
        // given
        Board board = Board.register("자유게시판", null);
        ReflectionTestUtils.setField(board, "id", 1L);

        given(boardRepository.findById(board.getId())).willReturn(Optional.of(board));
        given(postRepository.existsByBoardIdAndDeletedAtIsNull(board.getId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> boardAdminService.delete(board.getId()))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BoardExceptionEnum.BOARD_IN_USE.getMessage());
    }
}