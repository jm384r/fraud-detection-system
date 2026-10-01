package com.finance.service;

import com.finance.model.TransactionRecord;
import com.finance.model.UserAccount;
import com.finance.repository.TransactionRepository;
import com.finance.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class FraudDetectionService {

    private static final String PYTHON_AI_URL = "http://localhost:8000/api/v1/evaluate-fraud";

    private final TransactionRepository transactionRepository;
    private final Map<String, List<Long>> velocityCache = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    public FraudDetectionService(TransactionRepository transactionRepository,
                                 UserRepository userRepository,
                                 SimpMessagingTemplate messagingTemplate,
                                 Optional<StringRedisTemplate> redisTemplate) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate.orElse(null);
    }

    public Map<String, Object> evaluateTransaction(String accountId, double amount, String location, String timeOfDay, String channel) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. Fetch User Profile Baseline (Default to Raipur if not set)
        String homeLocation = "Raipur";
        Optional<UserAccount> userOpt = this.userRepository.findByUsername(accountId);
        if (userOpt.isPresent() && userOpt.get().getHomeLocation() != null) {
            homeLocation = userOpt.get().getHomeLocation();
        }

        // 2. Time-series Profiling from Database
        List<TransactionRecord> recentTxns = this.transactionRepository.findTop5ByAccountIdOrderByIdDesc(accountId);
        double historicalAverage = 0.0;
        if (!recentTxns.isEmpty()) {
            double total = 0.0;
            for (TransactionRecord record : recentTxns) {
                total += record.getAmount();
            }
            historicalAverage = total / recentTxns.size();
        }

// 3. New In-Memory Velocity Check (Transactions within 60 seconds)
        int recentSwipes = calculateVelocity(accountId);

        // 4. Construct Payload for Python Microservice
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("account_id", accountId);
        requestBody.put("amount", amount);
        requestBody.put("location", location);
        requestBody.put("time_of_day", timeOfDay);
        requestBody.put("home_location", homeLocation);
        requestBody.put("historical_avg", historicalAverage);
        requestBody.put("velocity_count", recentSwipes);
        requestBody.put("channel", channel != null ? channel : "UPI");

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(PYTHON_AI_URL, request, Map.class);
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

            if (responseBody != null) {
                double fraudScore = Double.parseDouble(responseBody.get("fraud_score").toString());
                String action = (String) responseBody.get("action");
                String reason = (String) responseBody.get("reason");

                // High-velocity penalty trigger (if swiped > 3 times in 60s)
                if (recentSwipes > 3 && !"BLOCK".equals(action)) {
                    action = "BLOCK";
                    fraudScore = Math.max(fraudScore, 0.88);
                    reason = "High velocity anomaly: Multiple rapid transactions detected within 60s. " + reason;
                    responseBody.put("action", action);
                    responseBody.put("fraud_score", fraudScore);
                    responseBody.put("reason", reason);
                }

                // Save to MySQL database
                TransactionRecord newRecord = new TransactionRecord(accountId, amount, location, timeOfDay, fraudScore, action, reason, channel);
                this.transactionRepository.save(newRecord);

                // Broadcast via WebSocket
                this.messagingTemplate.convertAndSend("/topic/transactions", newRecord);
            }

            return responseBody;

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR AI Offline: " + e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "AI Service Offline");
            fallback.put("action", "ALLOW");
            return fallback;
        }
    }
    private int calculateVelocity(String accountId) {
    long currentTime = System.currentTimeMillis();
    long windowStart = currentTime - 60000; // 60 seconds

    // Agar account pehli baar aaya hai toh list banao
    velocityCache.putIfAbsent(accountId, new CopyOnWriteArrayList<>());
    List<Long> timestamps = velocityCache.get(accountId);

    // Naya time record karo
    timestamps.add(currentTime);

    // 60 seconds (1 minute) se purane wale hata do
    timestamps.removeIf(time -> time < windowStart);

    // Total kitne bache? Wo count return kardo
    return timestamps.size();
}
}