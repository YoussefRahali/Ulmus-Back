package com.example.backpfe.Extraction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentFieldRepository extends JpaRepository<DocumentField, Long> {
    List<DocumentField> findByDocumentId(Long documentId);
}
