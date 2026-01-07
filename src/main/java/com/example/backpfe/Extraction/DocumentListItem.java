package com.example.backpfe.Extraction;

import java.time.Instant;

public record DocumentListItem(
        Long id,
        String fileName,
        Instant createdAt
) {}
