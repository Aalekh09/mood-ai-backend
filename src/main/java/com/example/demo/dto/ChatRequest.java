package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank
    private String message;

    private Boolean isAnonymous = false;

    // Optional: AI personality mode
    private String mode; // "supportive", "motivational", "analytical", "casual"
}