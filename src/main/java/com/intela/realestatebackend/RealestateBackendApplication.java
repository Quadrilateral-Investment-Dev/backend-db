package com.intela.realestatebackend;

import com.intela.realestatebackend.models.Token;
import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.archetypes.TokenType;
import com.intela.realestatebackend.repositories.TokenRepository;
import com.intela.realestatebackend.repositories.UserRepository;
import com.intela.realestatebackend.requestResponse.RegisterRequest;
import com.intela.realestatebackend.services.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Optional;

import static com.intela.realestatebackend.models.archetypes.Role.ADMIN;
import static com.intela.realestatebackend.models.archetypes.Role.CUSTOMER;

//TODO: Fix applications double-saving
@SpringBootApplication
public class RealestateBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealestateBackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(
            AuthService authenticationService,
            UserRepository userRepository,
            TokenRepository tokenRepository
    ) {
        return args -> {
            // Handle admin user
            String adminEmail = "kudzai@gmail.com";
            Optional<User> adminUser = userRepository.findByEmail(adminEmail);
            if (adminUser.isEmpty()) {
                var admin = RegisterRequest.builder()
                        .firstName("kudzai")
                        .lastName("matizirofa")
                        .mobileNumber("+2630772349201")
                        .email(adminEmail)
                        .password("1234")
                        .role(ADMIN)
                        .build();
                var savedAdmin = authenticationService.register(admin);
                System.out.println("Admin token: " + savedAdmin.getAccessToken());
                System.out.println("Admin refresh token: " + savedAdmin.getRefreshToken());
            } else {
                List<Token> tokens = tokenRepository.findAllValidTokenByUser(adminUser.get().getId());
                tokens.forEach(token -> {
                    if (token.getTokenType() == TokenType.ACCESS) {
                        System.out.println("Admin access token: " + token.getToken());
                    } else if (token.getTokenType() == TokenType.REFRESH) {
                        System.out.println("Admin refresh token: " + token.getToken());
                    }
                });
            }

            // Handle customer user
            String customerEmail = "nigel@gmail.com";
            Optional<User> customerUser = userRepository.findByEmail(customerEmail);
            if (customerUser.isEmpty()) {
                var customer = RegisterRequest.builder()
                        .firstName("nigel")
                        .lastName("nickel")
                        .mobileNumber("+2630772349201")
                        .email(customerEmail)
                        .password("1234")
                        .role(CUSTOMER)
                        .build();
                var savedCustomer = authenticationService.register(customer);
                System.out.println("Customer token: " + savedCustomer.getAccessToken());
                System.out.println("Customer refresh token: " + savedCustomer.getRefreshToken());
            } else {
                List<Token> tokens = tokenRepository.findAllValidTokenByUser(customerUser.get().getId());
                tokens.forEach(token -> {
                    if (token.getTokenType() == TokenType.ACCESS) {
                        System.out.println("Customer access token: " + token.getToken());
                    } else if (token.getTokenType() == TokenType.REFRESH) {
                        System.out.println("Customer refresh token: " + token.getToken());
                    }
                });
            }
        };
    }

}
