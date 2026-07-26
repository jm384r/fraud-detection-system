package com.finance;

import com.finance.model.UserAccount;
import com.finance.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Author: By Joel Mukherjee(20)
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    // Inject your existing repository
    public DatabaseSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        
        // Check if the database is empty before seeding
        if (userRepository.count() == 0) {
            
            // 1. Create the Admin Account
            UserAccount admin = new UserAccount();
            admin.setUsername("admin_manager");
            admin.setPassword("admin123"); 
            admin.setRole("ADMIN");
            admin.setHomeLocation("System Node"); 
            admin.setDailyLimit(1000000.0); // Adding a limit since you have this field!
            
            userRepository.save(admin);
            System.out.println(">>> Admin account seeded.");

            // 2. Create the Standard User Account
            UserAccount standardUser = new UserAccount();
            standardUser.setUsername("joel_client");
            standardUser.setPassword("pass123");
            standardUser.setRole("USER");
            standardUser.setHomeLocation("Raipur"); 
            standardUser.setDailyLimit(50000.0);
            
            userRepository.save(standardUser);
            System.out.println(">>> Standard User account seeded.");
        }
    }
}