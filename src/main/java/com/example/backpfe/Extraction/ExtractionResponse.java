package com.example.backpfe.Extraction;

public record ExtractionResponse(
        String fileName,
        String contentType,
        long size,
        String rawText
) {}
