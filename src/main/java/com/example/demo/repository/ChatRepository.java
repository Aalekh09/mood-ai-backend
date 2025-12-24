package com.example.demo.repository;

import com.example.demo.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Chat> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM Chat c WHERE c.user.id = ?1 ORDER BY c.createdAt DESC")
    List<Chat> findRecentChatsByUserId(Long userId);

    @Query("SELECT AVG(c.moodScore) FROM Chat c WHERE c.user.id = ?1")
    Double getAverageMoodScore(Long userId);

    @Query("SELECT c.sentiment, COUNT(c) FROM Chat c WHERE c.user.id = ?1 GROUP BY c.sentiment")
    List<Object[]> getSentimentDistribution(Long userId);
}