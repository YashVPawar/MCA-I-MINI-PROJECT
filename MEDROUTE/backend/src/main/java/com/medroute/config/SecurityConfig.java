package com.medroute.config;

import com.medroute.auth.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/health", "/api/auth/login", "/ws/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/routes/traffic").hasAuthority("ADMIN")
                        .requestMatchers("/api/routes/**").hasAnyAuthority("ADMIN", "AMBULANCE")
                        .requestMatchers(HttpMethod.POST, "/api/emergencies/rank-hospitals").hasAnyAuthority("ADMIN", "AMBULANCE")
                        .requestMatchers(HttpMethod.POST, "/api/emergencies").hasAuthority("AMBULANCE")
                        .requestMatchers(HttpMethod.GET, "/api/emergencies/**").hasAnyAuthority("ADMIN", "HOSPITAL")
                        .requestMatchers(HttpMethod.POST, "/api/emergencies/*/advance").hasAnyAuthority("ADMIN", "AMBULANCE", "HOSPITAL")
                        .requestMatchers("/api/emergencies/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/hospitals/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/hospitals/**").hasAnyAuthority("ADMIN", "HOSPITAL")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}