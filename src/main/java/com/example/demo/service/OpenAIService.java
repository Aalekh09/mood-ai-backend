package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OpenAIService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, List<Map<String, String>>> conversationMemory = new ConcurrentHashMap<>();

    public String getChatResponse(String userMessage, String userId) {
        try {
            log.info("🔵 ========================================");
            log.info("🔵 User Message: {}", userMessage);
            log.info("🔵 User ID: {}", userId);
            log.info("🔵 API URL: {}", apiUrl);

            String sentiment = analyzeSentiment(userMessage);
            log.info("🔵 Detected Sentiment: {}", sentiment);

            List<Map<String, String>> messages = buildConversation(userMessage, userId, sentiment);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.9);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("🚀 Calling Groq API...");

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            log.info("✅ Groq Response Status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String aiResponse = (String) message.get("content");

                    log.info("🎯 AI Response Generated: {} characters", aiResponse.length());
                    log.info("🎯 First 100 chars: {}", aiResponse.substring(0, Math.min(100, aiResponse.length())));

                    if (userId != null) {
                        storeConversation(userId, userMessage, aiResponse);
                    }

                    return aiResponse;
                }
            }

            log.warn("⚠️ No valid response from Groq, using fallback");
            return getFallbackResponse(sentiment);

        } catch (Exception e) {
            log.error("❌ Groq API Error: {}", e.getMessage());
            log.error("❌ Error Type: {}", e.getClass().getName());
            log.error("❌ Full Stack Trace: ", e);
            return getFallbackResponse(analyzeSentiment(userMessage));
        }
    }

    private List<Map<String, String>> buildConversation(String userMessage, String userId, String sentiment) {
        List<Map<String, String>> messages = new ArrayList<>();

        String systemPrompt = buildDynamicSystemPrompt(sentiment, userId);
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (userId != null && conversationMemory.containsKey(userId)) {
            List<Map<String, String>> history = conversationMemory.get(userId);
            int startIndex = Math.max(0, history.size() - 6);
            messages.addAll(history.subList(startIndex, history.size()));
        }

        messages.add(Map.of("role", "user", "content", userMessage));

        return messages;
    }

    private String buildDynamicSystemPrompt(String sentiment, String userId) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are Mood AI, a warm and caring mental wellness companion. ");
        prompt.append("Respond naturally like a supportive friend. Be specific and helpful.\n\n");

        switch (sentiment) {
            case "POSITIVE":
                prompt.append("The user is happy! Match their energy. Be enthusiastic. ");
                prompt.append("If they ask for songs, give 5-7 SPECIFIC song titles with artists. ");
                prompt.append("Example: '1. Happy by Pharrell Williams, 2. Good Vibrations by The Beach Boys'\n");
                break;

            case "NEGATIVE":
                prompt.append("The user is struggling. Be gentle and supportive. ");
                prompt.append("Offer specific help: breathing exercises, calming activities. ");
                prompt.append("If they ask for songs, give calming music with specific titles.\n");
                break;

            default:
                prompt.append("Be warm and conversational. ");
                prompt.append("When asked for recommendations, always give specific examples. ");
                prompt.append("If asked for songs, list actual song titles and artists.\n");
        }

        prompt.append("\nIMPORTANT: When the user asks for songs, ALWAYS provide a numbered list ");
        prompt.append("with specific song titles and artists. Never be vague!\n");
        prompt.append("Example:\n");
        prompt.append("1. 'Happy' by Pharrell Williams\n");
        prompt.append("2. 'Don't Stop Me Now' by Queen\n");
        prompt.append("3. 'Good Life' by OneRepublic\n\n");
        prompt.append("Keep responses friendly, specific, and under 200 words. Use 1-2 emojis.");

        return prompt.toString();
    }

    private void storeConversation(String userId, String userMessage, String aiResponse) {
        conversationMemory.putIfAbsent(userId, new ArrayList<>());
        List<Map<String, String>> history = conversationMemory.get(userId);

        history.add(Map.of("role", "user", "content", userMessage));
        history.add(Map.of("role", "assistant", "content", aiResponse));

        if (history.size() > 10) {
            history.subList(0, history.size() - 10).clear();
        }
    }

    public void clearConversationHistory(String userId) {
        conversationMemory.remove(userId);
    }

    public String analyzeSentiment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "NEUTRAL";
        }

        String lowerText = text.toLowerCase();

        String[] positiveWords = {
                "happy", "joy", "great", "excellent", "wonderful", "amazing", "fantastic",
                "excited", "grateful", "thankful", "blessed", "proud", "delighted",
                "cheerful", "love", "better", "good", "awesome"
        };

        String[] negativeWords = {
                "sad", "depressed", "angry", "anxious", "worried", "stressed", "upset",
                "frustrated", "hurt", "pain", "crying", "lonely", "hopeless", "scared",
                "afraid", "terrible", "horrible", "bad", "worse", "overwhelmed"
        };

        int positiveCount = 0;
        int negativeCount = 0;

        for (String word : positiveWords) {
            if (lowerText.contains(word)) {
                positiveCount++;
            }
        }

        for (String word : negativeWords) {
            if (lowerText.contains(word)) {
                negativeCount++;
            }
        }

        if (positiveCount > negativeCount && positiveCount > 0) {
            return "POSITIVE";
        } else if (negativeCount > positiveCount && negativeCount > 0) {
            return "NEGATIVE";
        }

        return "NEUTRAL";
    }

    public Double calculateMoodScore(String sentiment) {
        Random random = new Random();
        return switch (sentiment) {
            case "POSITIVE" -> 0.70 + (random.nextDouble() * 0.30);
            case "NEGATIVE" -> random.nextDouble() * 0.40;
            default -> 0.35 + (random.nextDouble() * 0.40);
        };
    }

    private String getFallbackResponse(String sentiment) {
        List<String> responses = switch (sentiment) {
            case "POSITIVE" -> List.of(
                    "That's wonderful! 🌟 I'm so happy to hear that! What specifically is making you feel this way?",
                    "Your positive energy is amazing! 😊 Tell me more about what's bringing you joy today!"
            );
            case "NEGATIVE" -> List.of(
                    "I hear you, and I'm here for you. 💙 What's weighing on your mind right now?",
                    "That sounds really tough. 💚 Would it help to talk about what's happening?"
            );
            default -> List.of(
                    "Thanks for sharing with me. 😊 How are you really feeling today?",
                    "I'm here to listen and help. 💭 What's on your mind?"
            );
        };

        return responses.get(new Random().nextInt(responses.size()));
    }
}