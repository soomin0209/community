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
    private String originalFilename;    // 원본 파일명

    @Column(nullable = false)
    private String storedPath;          // 저장된 경로

    @Column(nullable = false)
    private Long size;                  // 파일 크기

    @Column(nullable = false)
    private String contentType;         // MIME 타입

    public static File register(
            Long userId,
            String originalFilename,
            String storedPath,
            Long size,
            String contentType
    ) {
        File file = new File();

        file.userId = userId;
        file.originalFilename = originalFilename;
        file.storedPath = storedPath;
        file.size = size;
        file.contentType = contentType;

        return file;
    }

    public void attachToPost(Long postId) {
        this.postId = postId;
    }
}
