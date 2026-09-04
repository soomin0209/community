package com.community.domain.board.entity;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.dto.request.UpdateBoardRequest;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.user.enums.UserGrade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "boards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String name;

    @Enumerated(value = EnumType.STRING)
    private UserGrade minGrade;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static Board register(String name, UserGrade minGrade) {
        Board board = new Board();

        board.name = name;
        board.minGrade = minGrade;

        return board;
    }

    public void update(UpdateBoardRequest request) {
        if (request.name() == null && request.minGrade() == null) {
            throw new ServiceErrorException(BoardExceptionEnum.BOARD_UPDATE_NO_CONTENT);
        }
        if (request.name() != null) this.name = request.name();
        if (request.minGrade() != null) this.minGrade = request.minGrade();
    }
}
