package com.jobsphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/*
 AI Skill Matching Service — calls OpenAI ChatGPT API to match candidate skills
 against the job description and returns a structured match result.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiSkillMatchingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.api-url}")
    private String apiUrl;

    @Value("${app.openai.model}")
    private String model;

    @Value("${app.openai.enabled:false}")
    private boolean enabled;

    /*
     Returns a JSON string like:
     {"matchScore": 82, "matchedSkills": ["Java", "Spring Boot"], "missingSkills": ["Kubernetes"]}

     Returns null if AI is disabled or API call fails.
     */
    @Async
    public java.util.concurrent.CompletableFuture<AiMatchResult> matchSkills(
            String jobDescription, String requiredSkills, String candidateSkills) {

        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.debug("AI skill matching is disabled or API key not set");
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        try {
            String prompt = buildPrompt(jobDescription, requiredSkills, candidateSkills);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content",
                        "You are a recruitment assistant. Respond ONLY with valid JSON."),
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<?> choices = (List<?>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) choice.get("message");
                    String content = (String) message.get("content");
                    AiMatchResult result = objectMapper.readValue(content, AiMatchResult.class);
                    log.info("AI match score computed: {}", result.getMatchScore());
                    return java.util.concurrent.CompletableFuture.completedFuture(result);
                }
            }
        } catch (Exception e) {
            log.warn("AI skill matching failed (non-critical): {}", e.getMessage());
        }

        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }

    private String buildPrompt(String jobDescription, String requiredSkills, String candidateSkills) {
        return String.format("""
            Analyze this job application and respond ONLY with JSON in this exact format:
            {"matchScore": <0-100>, "matchedSkills": [<list of matching skills>], "missingSkills": [<list of missing skills>]}
            
            Job Description: %s
            Required Skills: %s
            Candidate Skills: %s
            """, jobDescription, requiredSkills, candidateSkills);
    }

    // Inner DTO for deserialization
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AiMatchResult {
        private Integer matchScore;
        private List<String> matchedSkills;
        private List<String> missingSkills;
    }
}
