// Author: By Joel Mukherjee(20)
package com.finance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;

    // NEW: Role column to dictate routing and permissions
    @Column(nullable = false)
    private String role;
    // ... previous variables (id, username, password, role) ...

    @Column(name = "home_location")
    private String homeLocation;

    @Column(name = "daily_limit")
    private Double dailyLimit;

    // Getters and Setters for the new variables
    public String getHomeLocation() { return homeLocation; }
    public void setHomeLocation(String homeLocation) { this.homeLocation = homeLocation; }
    public Double getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Double dailyLimit) { this.dailyLimit = dailyLimit; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}