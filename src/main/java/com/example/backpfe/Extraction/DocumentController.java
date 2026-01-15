package com.example.backpfe.Extraction;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "image/jpeg", // jpg + jpeg
            "image/png"
    );

    private final GeminiService geminiService;
    private final DocumentExtractionRepository repo;

    public DocumentController(GeminiService geminiService, DocumentExtractionRepository repo) {
        this.geminiService = geminiService;
        this.repo = repo;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtractionResponse extract(@RequestPart("file") MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "FILE_EMPTY");
        }

        String ct = file.getContentType();
        if (ct == null || !ALLOWED.contains(ct)) {
            throw new ResponseStatusException(BAD_REQUEST, "UNSUPPORTED_TYPE: " + ct);
        }

        String rawText = geminiService.extractRawText(file);

        DocumentExtraction saved = repo.save(new DocumentExtraction(
                file.getOriginalFilename(),
                ct,
                file.getSize(),
                rawText
        ));

        return new ExtractionResponse(
                saved.getId(),
                saved.getFileName(),
                saved.getContentType(),
                saved.getSize(),
                saved.getCreatedAt(),
                saved.getRawText()
        );
    }

    // GET /api/v1/documents  -> historique (sans rawText)
    @GetMapping
    public List<DocumentListItem> list() {
        return repo.findAllByOrderByIdDesc()
                .stream()
                .map(d -> new DocumentListItem(d.getId(), d.getFileName(), d.getCreatedAt()))
                .toList();
    }

    // GET /api/v1/documents/{id} -> détail (avec rawText)
    @GetMapping("/{id}")
    public ExtractionResponse getById(@PathVariable Long id) {
        DocumentExtraction d = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "NOT_FOUND id=" + id));

        return new ExtractionResponse(
                d.getId(),
                d.getFileName(),
                d.getContentType(),
                d.getSize(),
                d.getCreatedAt(),
                d.getRawText()
        );
    }
}
