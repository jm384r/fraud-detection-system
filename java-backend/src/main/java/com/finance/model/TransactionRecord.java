// Author: By Joel Mukherjee(20)
// Description: Database Entity representing a stored transaction.

package com.finance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "transactions")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Auto-generated primary key

    private String accountId;
    private Double amount;
    private String location;
    private String timeOfDay;
    private Double fraudScore;
    private String action;
    private String reason;
    private String channel;

    // Constructors
    public TransactionRecord() {}

// Fallback 6-argument constructor
    public TransactionRecord(String accountId, Double amount, String location, String timeOfDay, Double fraudScore, String action, String reason) {
    this.accountId = accountId;
    this.amount = amount;
    this.location = location;
    this.timeOfDay = timeOfDay;
    this.fraudScore = fraudScore;
    this.action = action;
    this.reason = reason;
    this.channel = "UPI";
}

// Full 7-argument constructor
    public TransactionRecord(String accountId, Double amount, String location, String timeOfDay, Double fraudScore, String action, String reason, String channel) {
    this.accountId = accountId;
    this.amount = amount;
    this.location = location;
    this.timeOfDay = timeOfDay;
    this.fraudScore = fraudScore;
    this.action = action;
    this.reason = reason;
    this.channel = (channel != null && !channel.trim().isEmpty()) ? channel : "UPI";
}

    // Getters and Setters (Omitted for brevity, but Spring/JPA uses them behind the scenes)
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public double getAmount() { return amount; }
    public String getLocation() { return location; }
    public String getTimeOfDay() { return timeOfDay; }
    public double getFraudScore() { return fraudScore; }
    public String getAction() { return action; }
    public String getChannel() {
        return channel;
    }
    public void setChannel(String channel) {
        this.channel = channel;
    }
}