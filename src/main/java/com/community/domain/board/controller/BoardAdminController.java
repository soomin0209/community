package com.community.domain.board.controller;

import com.community.common.dto.BaseResponse;
import com.community.domain.board.dto.request.CreateBoardRequest;
import com.community.domain.board.dto.request.UpdateBoardRequest;
import com.community.domain.board.dto.response.CreateBoardResponse;
import com.community.domain.board.dto.response.UpdateBoardResponse;
import com.community.domain.board.service.BoardAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards")
public class BoardAdminController {

    private final BoardAdminService boardAdminService;

    // 게시판 등록
    @PostMapping
    public ResponseEntity<BaseResponse<CreateBoardResponse>> create(
            @Valid @RequestBody CreateBoardRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), null, boardAdminService.create(request)));
    }

    // 게시판 수정
    @PatchMapping("/{boardId}")
    public ResponseEntity<BaseResponse<UpdateBoardResponse>> update(
            @PathVariable Long boardId,
            @Valid @RequestBody UpdateBoardRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, boardAdminService.update(boardId, request)));
    }

    // 게시판 삭제
    @DeleteMapping("/{boardId}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long boardId) {
        boardAdminService.delete(boardId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, null));
    }
}
