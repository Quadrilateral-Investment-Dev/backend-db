package com.intela.realestatebackend.config;

import com.intela.realestatebackend.handler.CustomLogoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static com.intela.realestatebackend.models.archetypes.Role.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CustomLogoutHandler logoutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests((authorize) -> authorize
                        // Public access to certain auth-related endpoints
                        .requestMatchers("/api/v1/auth/authenticate", "/api/v1/auth/register", "/api/v1/auth/forgotPassword")
                        .permitAll()

                        // Restrict these auth-related endpoints to authenticated users
                        .requestMatchers("/api/v1/auth/user", "/api/v1/auth/resetPassword")
                        .authenticated()

                        // Admin-only endpoints for user tokens
                        .requestMatchers("/api/v1/auth/userByAccessToken", "/api/v1/auth/userByRefreshToken")
                        .hasRole(ADMIN.name())

                        // Allow access to Swagger UI and API docs
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/swagger-ui.html", "/webjars/**")
                        .permitAll()

                        // Admin endpoints restricted to users with ADMIN role
                        .requestMatchers("/api/v1/admin/**").hasRole(ADMIN.name())

                        // Customer endpoints accessible by customers and admins
                        .requestMatchers("/api/v1/customer/**").hasAnyRole(CUSTOMER.name(), DEALER.name(), ADMIN.name())

                        // Dealer endpoints accessible by dealers and admins
                        .requestMatchers("/api/v1/dealer/**").hasAnyRole(DEALER.name(), ADMIN.name())

                        // Public access to property-related endpoints
                        .requestMatchers("/api/v1/properties/**").permitAll()

                        // User-related endpoints accessible by customers, dealers, and admins
                        .requestMatchers("/api/v1/user/**").hasAnyRole(CUSTOMER.name(), ADMIN.name(), DEALER.name())

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout((logout) -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler((request, response, authentication) ->
                                SecurityContextHolder.clearContext())
                );
        return http.build();
    }



    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
