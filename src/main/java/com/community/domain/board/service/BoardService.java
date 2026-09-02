package com.community.domain.board.service;

import com.community.domain.board.dto.response.GetAllBoardResponse;
import com.community.domain.board.entity.Board;
import com.community.domain.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;

    // 게시판 목록 조회
    public List<GetAllBoardResponse> getAll() {
        List<Board> boardList = boardRepository.findAll();

        return boardList.stream()
                .map(board -> new GetAllBoardResponse(
                        board.getId(),
                        board.getName()))
                .toList();
    }
}
