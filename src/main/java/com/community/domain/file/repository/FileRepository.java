package com.community.domain.file.repository;

import com.community.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findByIdAndDeletedAtIsNull(Long id);
}
