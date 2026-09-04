package br.com.dinamica.estoque.configuration.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class SHA256PasswordEncoderTest {

    private SHA256PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new SHA256PasswordEncoder();
    }

    @Test
    @DisplayName("Deve retornar true quando a senha aberta corresponder ao hash codificado")
    void matches_ShouldReturnTrue_WhenPasswordsMatch() {
        String rawPassword = "senhaSegura";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        assertTrue(matches);
    }

    @Test
    @DisplayName("Deve retornar false quando a senha aberta não corresponder ao hash codificado")
    void matches_ShouldReturnFalse_WhenPasswordsDoNotMatch() {
        String rawPassword = "senhaCorreta";
        String wrongPassword = "senhaIncorreta";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        assertFalse(matches);
    }

    @Test
    @DisplayName("Deve tratar NoSuchAlgorithmException e retornar null ao falhar ao obter a instância de MessageDigest")
    void encode_ShouldReturnNull_WhenNoSuchAlgorithmExceptionIsThrown() {
        try (MockedStatic<MessageDigest> mockedMessageDigest = mockStatic(MessageDigest.class)) {
            mockedMessageDigest.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("Algoritmo não encontrado"));

            String result = passwordEncoder.encode("qualquerSenha");

            assertNull(result);
        }
    }
}