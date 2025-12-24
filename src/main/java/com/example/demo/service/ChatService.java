package com.example.demo.service;

import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import com.example.demo.model.Chat;
import com.example.demo.model.User;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final OpenAIService openAIService;

    @Transactional
    public ChatResponse sendMessage(ChatRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Pass user ID for conversation context
        String aiResponse = openAIService.getChatResponse(
                request.getMessage(),
                user.getId().toString()  // Important: Pass user ID
        );

        String sentiment = openAIService.analyzeSentiment(request.getMessage());
        Double moodScore = openAIService.calculateMoodScore(sentiment);

        Chat chat = new Chat();
        chat.setUser(user);
        chat.setMessage(request.getMessage());
        chat.setResponse(aiResponse);
        chat.setSentiment(sentiment);
        chat.setMoodScore(moodScore);
        chat.setIsAnonymous(false);

        Chat savedChat = chatRepository.save(chat);

        return mapToChatResponse(savedChat);
    }
    @Transactional
    public ChatResponse sendAnonymousMessage(ChatRequest request) {
        // Get AI response (pass null for anonymous users - no conversation memory)
        String aiResponse = openAIService.getChatResponse(request.getMessage(), null);  // ✅ FIXED

        // Analyze sentiment
        String sentiment = openAIService.analyzeSentiment(request.getMessage());

        // Calculate mood score
        Double moodScore = openAIService.calculateMoodScore(sentiment);

        // For anonymous chats, we don't save to database
        return ChatResponse.builder()
                .message(request.getMessage())
                .response(aiResponse)
                .sentiment(sentiment)
                .moodScore(moodScore)
                .build();
    }

    public List<ChatResponse> getChatHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Chat> chats = chatRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return chats.stream()
                .map(this::mapToChatResponse)
                .collect(Collectors.toList());
    }

    public void deleteChat(Long chatId, String userEmail) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!chat.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this chat");
        }

        chatRepository.delete(chat);
    }

    private ChatResponse mapToChatResponse(Chat chat) {
        return ChatResponse.builder()
                .id(chat.getId())
                .message(chat.getMessage())
                .response(chat.getResponse())
                .sentiment(chat.getSentiment())
                .moodScore(chat.getMoodScore())
                .createdAt(chat.getCreatedAt())
                .build();
    }
}