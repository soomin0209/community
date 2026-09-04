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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardAdminService {

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;

    // 게시판 등록
    public CreateBoardResponse create(CreateBoardRequest request) {
        if (boardRepository.existsByName(request.name())) {
            throw new ServiceErrorException(BoardExceptionEnum.DUPLICATED_NAME);
        }

        Board board = Board.register(request.name(), request.minGrade());
        boardRepository.save(board);

        return new CreateBoardResponse(board.getId(), board.getName(), board.getCreatedAt());
    }

    // 게시판 수정
    public UpdateBoardResponse update(Long boardId, UpdateBoardRequest request) {
        Board board = boardRepository.findById(boardId).orElseThrow(
                () -> new ServiceErrorException(BoardExceptionEnum.BOARD_NOT_FOUND));

        if (request.name().equals(board.getName())) {
            throw new ServiceErrorException(BoardExceptionEnum.NAME_UNCHANGED);
        }

        if (boardRepository.existsByName(request.name())) {
            throw new ServiceErrorException(BoardExceptionEnum.DUPLICATED_NAME);
        }

        board.update(request.name());

        return new UpdateBoardResponse(
                board.getId(),
                board.getName(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }

    // 게시판 삭제
    public void delete(Long boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow(
                () -> new ServiceErrorException(BoardExceptionEnum.BOARD_NOT_FOUND));

        if (postRepository.existsByBoardIdAndDeletedAtIsNull(board.getId())) {
            throw new ServiceErrorException(BoardExceptionEnum.BOARD_IN_USE);
        }

        boardRepository.delete(board);
    }
}
