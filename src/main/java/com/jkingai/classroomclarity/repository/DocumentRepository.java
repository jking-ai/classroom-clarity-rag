package com.jkingai.classroomclarity.repository;

import com.jkingai.classroomclarity.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByCreatedAtBefore(OffsetDateTime cutoff);
}
