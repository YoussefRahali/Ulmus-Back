package com.example.backpfe.Extraction;

import jakarta.persistence.*;

@Entity
@Table(name = "document_fields")
public class DocumentField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentExtraction document;

    @Column(nullable = false)
    private String fieldKey;

    @Basic
    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;

    public DocumentField() {}

    public DocumentField(DocumentExtraction document, String fieldKey, String fieldValue) {
        this.document = document;
        this.fieldKey = fieldKey;
        this.fieldValue = fieldValue;
    }

    public Long getId() { return id; }
    public String getFieldKey() { return fieldKey; }
    public String getFieldValue() { return fieldValue; }
}
