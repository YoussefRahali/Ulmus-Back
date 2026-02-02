package com.example.backpfe.Extraction;

import java.util.Map;

public record StructuredResult(
        String docType,
        Map<String, String> fields,
        String rawText
) {}
