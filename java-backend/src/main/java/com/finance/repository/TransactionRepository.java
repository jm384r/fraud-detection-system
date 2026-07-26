// Author: By Joel Mukherjee(20)
package com.finance.repository;

import org.springframework.data.jpa.repository.Query;
import com.finance.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionRecord t WHERE t.action = 'BLOCK'")
    Double getTotalFraudPrevented();
    
    // NEW: Fetch the last 5 transactions for time-series profiling
    List<TransactionRecord> findTop5ByAccountIdOrderByIdDesc(String accountId);
}