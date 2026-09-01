package com.community.domain.board.controller;

import com.community.common.dto.BaseResponse;
import com.community.domain.board.dto.response.GetAllBoardResponse;
import com.community.domain.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    // 게시판 목록 조회
    @GetMapping
    public ResponseEntity<BaseResponse<List<GetAllBoardResponse>>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, boardService.getAll()));
    }
}
