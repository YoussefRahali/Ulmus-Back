package com.example.backpfe.Extraction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiService {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GeminiService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            WebClient.Builder builder
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = builder.baseUrl(baseUrl).build();    }

    public String extractRawText(MultipartFile file) throws IOException {

        // 1) Convertir en base64
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

        // 2) Prompt minimal stable
        String prompt = "Extract all text from this document. Return only the text.";

        // 3) Payload Gemini (Generative Language API)
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", base64
                                ))
                        ))
                )
        );

        // 4) Appel
        Map resp = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 5) Extraction texte (simple)
        return extractText(resp);
    }

    private String extractText(Map resp) {
        try {
            List candidates = (List) resp.get("candidates");
            Map c0 = (Map) candidates.get(0);
            Map content = (Map) c0.get("content");
            List parts = (List) content.get("parts");
            Map p0 = (Map) parts.get(0);
            return String.valueOf(p0.get("text"));
        } catch (Exception e) {
            return "ERROR_PARSING_RESPONSE: " + resp;
        }
    }
}

