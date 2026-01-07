package com.example.backpfe.Extraction;

import java.time.Instant;

public record ExtractionResponse(
        Long id,
        String fileName,
        String contentType,
        long size,
        Instant createdAt,
        String rawText
) {}
