package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.model.Chat;
import com.example.demo.model.User;
import com.example.demo.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/chats")
    public ResponseEntity<ApiResponse<List<Chat>>> getAllChats() {
        List<Chat> chats = adminService.getAllChats();
        return ResponseEntity.ok(ApiResponse.success(chats));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        try {
            adminService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/chats/{chatId}")
    public ResponseEntity<ApiResponse<Void>> deleteChat(@PathVariable Long chatId) {
        try {
            adminService.deleteChat(chatId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        Map<String, Object> analytics = adminService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/analytics/user/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserAnalytics(@PathVariable Long userId) {
        Map<String, Object> analytics = adminService.getUserAnalytics(userId);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}