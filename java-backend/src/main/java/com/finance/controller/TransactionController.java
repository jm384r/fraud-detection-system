// Author: By Joel Mukherjee(20)
// Description: REST Controller to handle incoming banking transactions.

package com.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.finance.model.TransactionRecord;
import com.finance.repository.TransactionRepository;
import com.finance.service.FraudDetectionService;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final FraudDetectionService fraudService;
    private final TransactionRepository transactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TransactionController(FraudDetectionService fraudService, 
                                 TransactionRepository transactionRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.fraudService = fraudService;
        this.transactionRepository = transactionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/process")
    public Map<String, Object> processTransaction(@RequestBody Map<String, Object> transactionData) {
        String accountId = (String) transactionData.get("accountId");
        double amount = Double.parseDouble(transactionData.get("amount").toString());
        String location = (String) transactionData.get("location");
        String timeOfDay = (String) transactionData.get("timeOfDay");
        String channel = transactionData.get("channel") != null 
                ? transactionData.get("channel").toString() 
                : "UPI_OFFLINE";

        // 1. Get the live AI verdict from Python
        Map<String, Object> result = fraudService.evaluateTransaction(accountId, amount, location, timeOfDay, channel);

        // 2. Broadcast transaction result to active WebSocket subscribers
        try {
            messagingTemplate.convertAndSend("/topic/transactions", (Object) result);
        } catch (Exception e) {
            System.err.println("WebSocket broadcast failed: " + e.getMessage());
        }

        // 3. Return the result to the simulator
        return result;
    }

    @GetMapping("/all")
    public ResponseEntity<List<TransactionRecord>> getAllTransactions() {
        // Fetches every transaction currently stored in the database
        List<TransactionRecord> allTransactions = transactionRepository.findAll();
        return ResponseEntity.ok(allTransactions);
    }
}