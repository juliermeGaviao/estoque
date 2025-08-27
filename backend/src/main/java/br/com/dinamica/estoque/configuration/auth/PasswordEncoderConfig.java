package br.com.dinamica.estoque.configuration.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    SHA256PasswordEncoder passwordEncoder() {
        return new SHA256PasswordEncoder();
    }

}
