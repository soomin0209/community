package com.community.domain.file.entity;

import com.community.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long postId;

    @Column(nullable = false)
    private String originalName;    // 원본 파일명

    @Column(nullable = false)
    private String storedPath;      // 저장된 경로

    @Column(nullable = false)
    private Long size;              // 파일 크기

    @Column(nullable = false)
    private String type;            // MIME 타입

    public static File register(
            Long userId,
            String originalName,
            String storedPath,
            Long size,
            String type
    ) {
        File file = new File();

        file.userId = userId;
        file.originalName = originalName;
        file.storedPath = storedPath;
        file.size = size;
        file.type = type;

        return file;
    }
}
