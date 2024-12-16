package com.valyalkin.market.providers.marketstack.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CustomLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException {
        String date = jsonParser.getText();
        return LocalDate.parse(date, formatter); // Only date part will be extracted
    }
}
