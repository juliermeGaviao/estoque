package br.com.dinamica.estoque.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

@ExtendWith(MockitoExtension.class)
class SafeDateDeserializerTest {

    @Mock
    private JsonParser jsonParser;

    @Mock
    private DeserializationContext deserializationContext;

    private SafeDateDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new SafeDateDeserializer();
    }

    @Test
    @DisplayName("Deve garantir instanciação da classe")
    void constructorTest() {
        assertNotNull(deserializer);
    }

    @Test
    @DisplayName("Deve converter string válida no formato dd/MM/yyyy para LocalDate com sucesso")
    void deserialize_ShouldReturnLocalDate_WhenValidDateString() throws IOException {
        when(jsonParser.getText()).thenReturn("25/12/2026");

        LocalDate result = deserializer.deserialize(jsonParser, deserializationContext);

        assertEquals(LocalDate.of(2026, 12, 25), result);
    }

    @Test
    @DisplayName("Deve remover espaços nas extremidades (trim) e converter a data com sucesso")
    void deserialize_ShouldTrimAndReturnLocalDate_WhenDateStringHasWhitespace() throws IOException {
        when(jsonParser.getText()).thenReturn(" 01/01/2026 ");

        LocalDate result = deserializer.deserialize(jsonParser, deserializationContext);

        assertEquals(LocalDate.of(2026, 1, 1), result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Deve retornar null quando a entrada for null, vazia ou contiver apenas espaços em branco")
    void deserialize_ShouldReturnNull_WhenTextIsNullOrEmptyOrBlank(String text) throws IOException {
        when(jsonParser.getText()).thenReturn(text);

        LocalDate result = deserializer.deserialize(jsonParser, deserializationContext);

        assertNull(result);
    }

    @Test
    @DisplayName("Deve retornar null e tratar IOException quando p.getText() falhar")
    void deserialize_ShouldReturnNull_WhenIOExceptionOccurs() throws IOException {
        when(jsonParser.getText()).thenThrow(new IOException("Erro ao ler JSON"));

        LocalDate result = deserializer.deserialize(jsonParser, deserializationContext);

        assertNull(result);
    }

}