package com.jkingai.classroomclarity.repository;

import com.jkingai.classroomclarity.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Query(value = """
            SELECT dc.* FROM document_chunks dc
            WHERE 1 - (dc.embedding <=> CAST(:embedding AS vector)) >= :threshold
            ORDER BY dc.embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("topK") int topK);

    @Query(value = """
            SELECT dc.* FROM document_chunks dc
            WHERE dc.document_id = ANY(CAST(:documentIds AS uuid[]))
              AND 1 - (dc.embedding <=> CAST(:embedding AS vector)) >= :threshold
            ORDER BY dc.embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunksFilteredByDocuments(
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("topK") int topK,
            @Param("documentIds") UUID[] documentIds);
}
