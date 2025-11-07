package br.com.dinamica.estoque.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SafeDateDeserializer extends JsonDeserializer<Date> {

    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();

        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            return this.formatter.parse(text);
        } catch (ParseException e) {
            return null;
        }
    }
}
