// Author: By Joel Mukherjee(20)
// Description: REST Controller to handle incoming banking transactions.

package com.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public TransactionController(FraudDetectionService fraudService, TransactionRepository transactionRepository) {
        this.fraudService = fraudService;
        this.transactionRepository = transactionRepository;
    }

@PostMapping("/process")
    public Map<String, Object> processTransaction(@RequestBody Map<String, Object> transactionData) {
        String accountId = (String) transactionData.get("accountId");
        double amount = Double.parseDouble(transactionData.get("amount").toString());
        String location = (String) transactionData.get("location");
        String timeOfDay = (String) transactionData.get("timeOfDay");

        // 1. Get the live AI verdict from Python
        Map<String, Object> result = fraudService.evaluateTransaction(accountId, amount, location, timeOfDay);

        // 4. Return the result to the frontend
        return result;
    }
    @GetMapping("/all")
    public ResponseEntity<List<TransactionRecord>> getAllTransactions() {
        // Fetches every transaction currently stored in the database
        List<TransactionRecord> allTransactions = transactionRepository.findAll();
        return ResponseEntity.ok(allTransactions);
    }
}