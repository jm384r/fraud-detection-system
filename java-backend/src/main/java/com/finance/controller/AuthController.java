// Author: By Joel Mukherjee(20)
package com.finance.controller;

import com.finance.model.UserAccount;
import com.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody UserAccount newUser) {
        Map<String, String> response = new HashMap<>();
        
        // Check if username is taken
        if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
            response.put("status", "error");
            response.put("message", "Username already exists.");
            return ResponseEntity.badRequest().body(response);
        }

        // Logic to assign Admin vs standard User. 
        // For demonstration, if they type "admin" in their username, they get admin rights.
        if (newUser.getUsername().toLowerCase().contains("admin")) {
            newUser.setRole("ADMIN");
        } else {
            newUser.setRole("USER");
        }
        
        userRepository.save(newUser);
        
        // Master Plan Route 3: Send new users to the onboarding page
        response.put("status", "success");
        response.put("role", newUser.getRole());
        response.put("redirect", "onboarding.html"); 
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody UserAccount loginRequest) {
        Map<String, String> response = new HashMap<>();
        Optional<UserAccount> userOpt = userRepository.findByUsername(loginRequest.getUsername());

        // Verify password (In production, this would use BCrypt hashing)
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(loginRequest.getPassword())) {
            UserAccount user = userOpt.get();
            response.put("status", "success");
            response.put("role", user.getRole());
            
            // Master Plan Route 1 & 2: Dynamic RBAC Routing
            if ("ADMIN".equals(user.getRole())) {
                response.put("redirect", "metrics.html"); // Admin goes to Analytics
            } else {
                response.put("redirect", "my-account.html"); // Returning user goes to their account
            }
            return ResponseEntity.ok(response);
        }

        response.put("status", "error");
        response.put("message", "Invalid credentials. Please try again.");
        return ResponseEntity.status(401).body(response);
    }
        @PostMapping("/onboard")
    public ResponseEntity<Map<String, String>> completeOnboarding(@RequestBody Map<String, Object> payload) {
        Map<String, String> response = new HashMap<>();
        
        String username = (String) payload.get("username");
        String location = (String) payload.get("homeLocation");
        // Convert the incoming number to a Double safely
        Double limit = Double.valueOf(payload.get("dailyLimit").toString());

        Optional<UserAccount> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            UserAccount user = userOpt.get();
            user.setHomeLocation(location);
            user.setDailyLimit(limit);
            userRepository.save(user); // Hibernate automatically updates the existing row!

            response.put("status", "success");
            response.put("message", "Profile baseline secured.");
            return ResponseEntity.ok(response);
        }

        response.put("status", "error");
        response.put("message", "User not found.");
        return ResponseEntity.badRequest().body(response);
    }
}