package br.com.dinamica.estoque.configuration.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PasswordEncoderConfigTest {

    private PasswordEncoderConfig passwordEncoderConfig;

    @BeforeEach
    void setUp() {
        passwordEncoderConfig = new PasswordEncoderConfig();
    }

    @Test
    @DisplayName("Deve instanciar o Bean SHA256PasswordEncoder com sucesso")
    void shouldReturnSHA256PasswordEncoderBean() {
        // Act
        SHA256PasswordEncoder encoder = passwordEncoderConfig.passwordEncoder();

        // Assert
        assertNotNull(encoder, "O Bean do password encoder não deve ser nulo");
        assertInstanceOf(SHA256PasswordEncoder.class, encoder, "Deveria retornar uma instância de SHA256PasswordEncoder");
    }
}