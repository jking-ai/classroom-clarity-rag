package com.jkingai.classroomclarity.repository;

import com.jkingai.classroomclarity.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Modifying
    @Query(value = "DELETE FROM document_chunks WHERE document_id = :documentId", nativeQuery = true)
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    @Query(value = """
            SELECT dc.id, dc.document_id, dc.content, dc.page_number,
                   dc.chunk_index, dc.token_count, dc.created_at,
                   NULL::real[] AS embedding,
                   1 - (dc.embedding <=> CAST(:embedding AS vector)) AS similarity_score
            FROM document_chunks dc
            WHERE 1 - (dc.embedding <=> CAST(:embedding AS vector)) >= :threshold
            ORDER BY dc.embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findSimilarChunksRaw(
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("topK") int topK);

    @Query(value = """
            SELECT dc.id, dc.document_id, dc.content, dc.page_number,
                   dc.chunk_index, dc.token_count, dc.created_at,
                   NULL::real[] AS embedding,
                   1 - (dc.embedding <=> CAST(:embedding AS vector)) AS similarity_score
            FROM document_chunks dc
            WHERE dc.document_id = ANY(CAST(:documentIds AS uuid[]))
              AND 1 - (dc.embedding <=> CAST(:embedding AS vector)) >= :threshold
            ORDER BY dc.embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findSimilarChunksFilteredByDocumentsRaw(
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("topK") int topK,
            @Param("documentIds") UUID[] documentIds);
}
