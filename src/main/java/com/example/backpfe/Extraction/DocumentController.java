package com.example.backpfe.Extraction;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final GeminiService geminiService;
    private final DocumentExtractionRepository repo;

    public DocumentController(GeminiService geminiService, DocumentExtractionRepository repo) {
        this.geminiService = geminiService;
        this.repo = repo;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtractionResponse extract(@RequestPart("file") MultipartFile file) throws IOException {

        String rawText;
        try {
            rawText = geminiService.extractRawText(file);
        } catch (Exception e) {
            throw new RuntimeException("GEMINI_CALL_FAILED: " + e.getMessage(), e);
        }

        repo.save(new DocumentExtraction(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                rawText
        ));

        return new ExtractionResponse(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                rawText
        );
    }
}
