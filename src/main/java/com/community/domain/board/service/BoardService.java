package com.community.domain.board.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.dto.response.GetAllBoardResponse;
import com.community.domain.board.entity.Board;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.board.repository.BoardRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    // 게시판 목록 조회
    public List<GetAllBoardResponse> getAll() {
        List<Board> boardList = boardRepository.findAll();

        return boardList.stream()
                .map(board -> new GetAllBoardResponse(
                        board.getId(),
                        board.getName()))
                .toList();
    }

    // 게시판 접근 권한 검증
    public void validateBoardAccess(Long userId, Long boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow(
                () -> new ServiceErrorException(BoardExceptionEnum.BOARD_NOT_FOUND));

        if (board.getMinGrade() == null) {
            return;
        }

        if (userId == null) {
            throw new ServiceErrorException(BoardExceptionEnum.BOARD_ACCESS_DENIED);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (user.getGrade().getLevel() < board.getMinGrade().getLevel()) {
            throw new ServiceErrorException(BoardExceptionEnum.BOARD_ACCESS_DENIED);
        }
    }
}
