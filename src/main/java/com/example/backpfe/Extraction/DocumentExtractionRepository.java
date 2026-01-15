package com.example.backpfe.Extraction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, Long> {
    List<DocumentExtraction> findAllByOrderByIdDesc();
}
