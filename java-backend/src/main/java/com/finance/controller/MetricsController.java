package com.finance.controller;

import com.finance.repository.TransactionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final TransactionRepository transactionRepository;

    public MetricsController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/global")
    public Map<String, Object> getGlobalMetrics() {
        // 1. Get the total number of transactions scanned
        long totalScanned = transactionRepository.count();

        // 2. Calculate the total fraud prevented using your custom repository query
        Double fraudPrevented = transactionRepository.getTotalFraudPrevented();
        
        // Safety check: if the database has no blocked transactions yet, default to 0
        if (fraudPrevented == null) {
            fraudPrevented = 0.0;
        }

        // 3. Package it into a JSON object for the frontend
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("fraudPrevented", fraudPrevented);
        metrics.put("totalScanned", totalScanned);

        return metrics;
    }
}