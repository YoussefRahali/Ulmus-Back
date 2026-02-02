package com.example.backpfe.Extraction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

import java.io.IOException;
import java.util.*;

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
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public String extractRawText(MultipartFile file) throws IOException {

        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

        String prompt = "Extract all text from this document. Return only the text.";

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

        Map resp;
        try {
            resp = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(BAD_GATEWAY,
                    "GEMINI_CALL_FAILED " + e.getStatusCode().value() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "GEMINI_UNAVAILABLE " + e.getMessage());
        }

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
            throw new ResponseStatusException(BAD_GATEWAY, "GEMINI_BAD_RESPONSE " + resp);
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public StructuredResult extractStructured(MultipartFile file) throws IOException {

        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

        // ===== PROMPT AMÉLIORÉ AVEC CHAMPS SPÉCIFIQUES PAR TYPE =====
        String prompt = """
    You are an OCR + document understanding engine.
    
    Task:
    1) First, detect the document type among: CHEQUE, FACTURE, CONTRAT, CONVENTION, AUTRE.
    2) Then extract ONLY the relevant fields for that specific document type with their EXACT values.
    
    For each document type, extract these specific fields:
    
    **CHEQUE:**
    - Amount (numeric value)
    - AmountInWords (amount written in letters)
    - Date (format: DD/MM/YYYY or DD-MM-YYYY)
    - Payee (beneficiary name)
    - BankName (name of the bank)
    - CheckNumber (check number)
    - AccountNumber (account number if visible)
    - Signature (write "Present" or "Absent")
    
    **FACTURE (Invoice):**
    - InvoiceNumber (numéro de facture)
    - InvoiceDate (date de facture, format: DD/MM/YYYY)
    - DueDate (date d'échéance if present)
    - SupplierName (nom du fournisseur)
    - SupplierAddress (adresse du fournisseur)
    - ClientName (nom du client)
    - ClientAddress (adresse du client)
    - TotalAmountHT (montant hors taxes)
    - TotalTVA (montant TVA)
    - TotalAmountTTC (montant toutes taxes comprises)
    - Currency (devise: EUR, USD, TND, etc.)
    - Items (list of items as JSON array if multiple items)
    
    **CONTRAT (Contract):**
    - ContractNumber (numéro du contrat)
    - ContractDate (date du contrat)
    - ContractType (type de contrat: travail, location, etc.)
    - Party1Name (nom de la première partie)
    - Party1Address (adresse de la première partie)
    - Party2Name (nom de la deuxième partie)
    - Party2Address (adresse de la deuxième partie)
    - StartDate (date de début)
    - EndDate (date de fin)
    - Amount (montant if applicable)
    - Duration (durée)
    - SignatureDate (date de signature)
    
    **CONVENTION:**
    - ConventionTitle (titre de la convention)
    - ConventionNumber (numéro)
    - ConventionDate (date)
    - Organization1 (première organisation)
    - Organization2 (deuxième organisation)
    - Purpose (objet de la convention)
    - StartDate (date de début)
    - EndDate (date de fin)
    - SignatureDate (date de signature)
    
    **AUTRE:**
    Extract any key fields you can identify with descriptive names.
    
    Output MUST be valid JSON only (no markdown, no code blocks, no extra text), with this exact structure:
    {
      "docType": "CHEQUE",
      "fields": {
        "Amount": "1500.00",
        "Date": "15/01/2026",
        "Payee": "John Doe"
      },
      "rawText": "full text extracted from document"
    }
    
    IMPORTANT:
    - Extract ONLY the fields relevant to the detected document type
    - If a field is not found or not visible, omit it completely (do not include it)
    - Use the exact field names provided above
    - All values must be strings
    - The rawText field should contain all the text extracted from the document
    """;

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

        Map resp;
        try {
            resp = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "GEMINI_CALL_FAILED " + e.getMessage());
        }

        String text = extractText(resp);

        // ===== CLEAN GEMINI RESPONSE =====
        text = text.trim();

        // Remove markdown code blocks if present
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            int lastFence = text.lastIndexOf("```");
            if (lastFence >= 0) {
                text = text.substring(0, lastFence);
            }
            text = text.trim();
        }

        // Remove "json" label if present
        if (text.startsWith("json")) {
            text = text.substring(4).trim();
        }
        // =================================

        // Parse JSON strict
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(text);

            String docType = root.path("docType").asText("AUTRE");

            Map<String, String> fields = new HashMap<>();
            JsonNode f = root.path("fields");

            if (f.isObject()) {
                f.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    JsonNode val = entry.getValue();

                    // ===== FIX: CONVERSION CORRECTE DES VALEURS =====
                    String value;
                    if (val == null || val.isNull()) {
                        value = "";
                    } else if (val.isTextual()) {
                        // C'est déjà une chaîne de caractères
                        value = val.asText();
                    } else if (val.isNumber()) {
                        // Convertir le nombre en chaîne
                        value = val.asText();
                    } else if (val.isBoolean()) {
                        // Convertir le booléen en chaîne
                        value = val.asText();
                    } else if (val.isArray() || val.isObject()) {
                        // Pour les tableaux/objets, conserver en JSON
                        value = val.toString();
                    } else {
                        // Cas par défaut
                        value = val.toString();
                    }

                    fields.put(key, value);
                });
            }

            String rawText = root.path("rawText").asText("");

            return new StructuredResult(docType, fields, rawText);

        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY,
                    "GEMINI_JSON_PARSE_FAILED " + ex.getMessage() + " :: Raw response: " + text);
        }
    }
}