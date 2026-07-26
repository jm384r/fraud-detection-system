// Author: By Joel Mukherjee(20)
// Description: Service class to bridge Java with Python and save to the Database.

package com.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.finance.repository.TransactionRepository;
import com.finance.model.TransactionRecord;
import java.util.HashMap;
import java.util.Map;
import com.finance.repository.UserRepository;
import com.finance.model.UserAccount;
import java.util.Optional;
import java.util.List;

@Service
public class FraudDetectionService {

    // Added 'static' to clear the IDE warning
    private static final String PYTHON_AI_URL = "http://localhost:8000/api/v1/evaluate-fraud";
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public FraudDetectionService(TransactionRepository transactionRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }
    @SuppressWarnings({"unchecked", "rawtypes"}) // Tells the IDE not to panic about the Python JSON conversion
    public Map<String, Object> evaluateTransaction(String accountId, double amount, String location, String timeOfDay) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 1. Fetch user's profile for Home Location
        String homeLocation = "Raipur";
        Optional<UserAccount> userOpt = userRepository.findByUsername(accountId);
        if (userOpt.isPresent() && userOpt.get().getHomeLocation() != null) {
            homeLocation = userOpt.get().getHomeLocation();
        }

        // 2. Fetch Account History for Time-Series Profiling
        List<TransactionRecord> recentTxns = transactionRepository.findTop5ByAccountIdOrderByIdDesc(accountId);
        
        double historicalAverage = 0.0;
        if (!recentTxns.isEmpty()) {
            double total = 0;
            for (TransactionRecord record : recentTxns) {
                total += record.getAmount();
            }
            historicalAverage = total / recentTxns.size();
        }
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("account_id", accountId);
        requestBody.put("amount", amount);
        requestBody.put("location", location);
        requestBody.put("time_of_day", timeOfDay);
        requestBody.put("home_location", homeLocation);       
        requestBody.put("historical_avg", historicalAverage);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(PYTHON_AI_URL, request, Map.class);
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

            if (responseBody != null) {
                double fraudScore = Double.parseDouble(responseBody.get("fraud_score").toString());
                String action = (String) responseBody.get("action");

                String reason = (String) responseBody.get("reason");

                TransactionRecord newRecord = new TransactionRecord(
                    accountId, amount, location, timeOfDay, fraudScore, action, reason
                );

                transactionRepository.save(newRecord);
                messagingTemplate.convertAndSend("/topic/transactions", newRecord);
            }

            return responseBody;

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR - AI Offline: " + e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "AI Service Offline");
            fallback.put("action", "ALLOW"); 
            return fallback;
        }
    }
}