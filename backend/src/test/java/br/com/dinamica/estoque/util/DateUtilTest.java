package br.com.dinamica.estoque.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DateUtilTest {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    @Test
    @DisplayName("Deve instanciar o construtor privado via reflection para garantir 100% de cobertura")
    void constructorTest() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<DateUtil> constructor = DateUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        DateUtil instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("Deve retornar data e hora atual entre o intervalo de execução")
    void deveRetornarDataHoraAtual() {
        LocalDateTime antes = ZonedDateTime.now(ZONE_ID).toLocalDateTime();
        LocalDateTime resultado = DateUtil.now();
        LocalDateTime depois = ZonedDateTime.now(ZONE_ID).toLocalDateTime();

        assertThat(resultado)
                .isNotNull()
                .isAfterOrEqualTo(antes)
                .isBeforeOrEqualTo(depois);
    }

    @Test
    @DisplayName("Deve retornar data e hora no fuso de São Paulo")
    void deveRetornarDataHoraNoFusoDeSaoPaulo() {
        LocalDateTime resultado = DateUtil.now();
        LocalDateTime esperado = ZonedDateTime.now(ZONE_ID).toLocalDateTime();

        long diferencaEmMilissegundos = Math.abs(Duration.between(esperado, resultado).toMillis());

        assertThat(diferencaEmMilissegundos).isLessThan(1000L);
    }
}