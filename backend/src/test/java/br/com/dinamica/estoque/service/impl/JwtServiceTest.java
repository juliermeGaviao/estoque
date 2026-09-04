package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;
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

}