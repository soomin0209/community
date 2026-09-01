package com.community.domain.file.repository;

import com.community.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findByIdAndDeletedAtIsNull(Long id);

    List<File> findByPostIdAndDeletedAtIsNull(Long postId);

    List<File> findByPostIdIsNullAndCreatedAtBefore(LocalDateTime threshold);

    List<File> findByDeletedAtBefore(LocalDateTime threshold);

    List<File> findAllByIdInAndDeletedAtIsNull(List<Long> ids);
}
