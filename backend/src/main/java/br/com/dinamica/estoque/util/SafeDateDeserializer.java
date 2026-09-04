package br.com.dinamica.estoque.util;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SafeDateDeserializer extends JsonDeserializer<LocalDate> {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) {
        String text = null;

        try {
			text = p.getText();
		} catch (IOException e) {
			log.error("Falha na obtenção do texto da data");
		}

        if (text == null || text.isBlank()) {
            return null;
        }

        return LocalDate.parse(text.trim(), formatter);
    }
}
