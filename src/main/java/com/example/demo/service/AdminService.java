package com.example.demo.service;

import com.example.demo.model.Chat;
import com.example.demo.model.User;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<Chat> getAllChats() {
        return chatRepository.findAll();
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public void deleteChat(Long chatId) {
        chatRepository.deleteById(chatId);
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalChats = chatRepository.count();

        List<Object[]> sentimentDist = chatRepository
                .getSentimentDistribution(null); // For all users

        analytics.put("totalUsers", totalUsers);
        analytics.put("totalChats", totalChats);
        analytics.put("sentimentDistribution", sentimentDist);

        return analytics;
    }

    public Map<String, Object> getUserAnalytics(Long userId) {
        Map<String, Object> analytics = new HashMap<>();

        Double avgMoodScore = chatRepository.getAverageMoodScore(userId);
        List<Object[]> sentimentDist = chatRepository.getSentimentDistribution(userId);
        List<Chat> recentChats = chatRepository.findRecentChatsByUserId(userId);

        analytics.put("averageMoodScore", avgMoodScore != null ? avgMoodScore : 0.0);
        analytics.put("sentimentDistribution", sentimentDist);
        analytics.put("totalChats", recentChats.size());

        return analytics;
    }
}