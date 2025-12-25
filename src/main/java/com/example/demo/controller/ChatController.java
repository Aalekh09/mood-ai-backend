package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ChatService;
import com.example.demo.service.OpenAIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final OpenAIService openAIService;
    private final UserRepository userRepository;  // Add this line

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized"));
        }

        String email = authentication.getName();
        ChatResponse response = chatService.sendMessage(request, email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @PostMapping("/anonymous")
    public ResponseEntity<ApiResponse<ChatResponse>> sendAnonymousMessage(
            @Valid @RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatService.sendAnonymousMessage(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getChatHistory(
            Authentication authentication) {
        try {
            String email = authentication.getName();
            List<ChatResponse> history = chatService.getChatHistory(email);
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<ApiResponse<Void>> deleteChat(
            @PathVariable Long chatId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            chatService.deleteChat(chatId, email);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/clear-context")
    public ResponseEntity<ApiResponse<String>> clearConversationContext(
            Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            openAIService.clearConversationHistory(user.getId().toString());

            return ResponseEntity.ok(ApiResponse.success("Conversation context cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}