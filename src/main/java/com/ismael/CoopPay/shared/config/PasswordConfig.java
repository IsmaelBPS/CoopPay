package com.ismael.CoopPay.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Defaults recomendados pelo Spring Security 5.8+ (Argon2id, 16B salt, 32B hash, 1 thread, 16MB, 2 iterações)
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
