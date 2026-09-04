package br.com.dinamica.estoque.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

class DateUtilTest {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    @Test
    void deveRetornarDataHoraAtual() {
        LocalDateTime antes = ZonedDateTime.now(ZONE_ID).toLocalDateTime();

        LocalDateTime resultado = DateUtil.now();

        LocalDateTime depois = ZonedDateTime.now(ZONE_ID).toLocalDateTime();

        assertNotNull(resultado);

        assertTrue(
            !resultado.isBefore(antes) && !resultado.isAfter(depois),
            "A data/hora retornada deve estar entre antes e depois da chamada"
        );
    }

    @Test
    void deveRetornarDataHoraNoFusoDeSaoPaulo() {
        LocalDateTime resultado = DateUtil.now();

        LocalDateTime esperado = ZonedDateTime
            .now(ZONE_ID)
            .toLocalDateTime();

        long diferencaEmMilissegundos = Math.abs(
            Duration.between(esperado, resultado).toMillis()
        );

        assertTrue(
            diferencaEmMilissegundos < 1000,
            "A diferença entre os horários deve ser inferior a 1 segundo"
        );
    }
}