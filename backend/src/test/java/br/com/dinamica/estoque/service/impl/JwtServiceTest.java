package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private JwtService jwtService;

    // A chave secreta HMAC-SHA precisa ter no mínimo 256 bits (32 caracteres)
    private final String secretKey = "chave_secreta_super_segura_para_testes_de_unidade_123456";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secretKey);
    }

    @Test
    @DisplayName("gerarToken e getSubject - deve gerar token valido e extrair o subject corretamente")
    void gerarTokenEGetSubject_shouldGenerateAndRetrieveSubject() {
        String expectedSubject = "usuario@empresa.com";

        String token = jwtService.gerarToken(expectedSubject);

        assertNotNull(token);
        String actualSubject = jwtService.getSubject(token);
        assertEquals(expectedSubject, actualSubject);
    }

    @Test
    @DisplayName("getSubject - deve lancar excecao ao tentar validar token assinado com chave invalida")
    void getSubject_shouldThrowExceptionForInvalidSignature() {
        String subject = "admin";
        String token = jwtService.gerarToken(subject);

        // Instancia outro service com chave diferente para simular assinatura inválida
        JwtService invalidKeyService = new JwtService();
        ReflectionTestUtils.setField(invalidKeyService, "jwtSecret", "outra_chave_secreta_diferente_com_32_caracteres_minimo_123");

        assertThrows(JwtException.class, () -> invalidKeyService.getSubject(token));
    }
}