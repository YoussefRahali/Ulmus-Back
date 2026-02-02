package com.example.backpfe.Extraction;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "document_extractions")
public class DocumentExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String contentType;
    private long size;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawText;

    // nouveau
    private String docType; // CHEQUE / FACTURE / CONTRAT ...

    // optionnel: stocker le JSON complet
    @Lob
    @Column(columnDefinition = "TEXT")
    private String structuredJson;

    private Instant createdAt = Instant.now();

    public DocumentExtraction() {}

    public DocumentExtraction(String fileName, String contentType, long size, String rawText, String docType, String structuredJson) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.rawText = rawText;
        this.docType = docType;
        this.structuredJson = structuredJson;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public String getRawText() { return rawText; }
    public Instant getCreatedAt() { return createdAt; }
    public String getDocType() { return docType; }
    public String getStructuredJson() { return structuredJson; }
}
