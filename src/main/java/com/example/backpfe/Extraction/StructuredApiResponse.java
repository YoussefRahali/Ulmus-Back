package com.example.backpfe.Extraction;

import java.util.Map;

public record StructuredApiResponse(
        Long documentId,
        String docType,
        Map<String, String> fields
) {}
