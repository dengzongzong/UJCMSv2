package com.exam.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(FORMATTER));
        module.addDeserializer(LocalDateTime.class, new MultiFormatLocalDateTimeDeserializer());
        
        mapper.registerModule(module);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 容错: 反序列化时遇到未知字段不报错(避免前端新增字段/新旧端不一致导致 400)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return mapper;
    }

    public static class MultiFormatLocalDateTimeDeserializer extends LocalDateTimeDeserializer {
        private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        public MultiFormatLocalDateTimeDeserializer() {
            super(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser parser, 
                                          com.fasterxml.jackson.databind.DeserializationContext context) 
                                          throws java.io.IOException {
            String value = parser.getText().trim();
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    return LocalDateTime.parse(value, formatter);
                } catch (DateTimeParseException e) {
                    // try next format
                }
            }
            throw new java.io.IOException("Cannot parse date: " + value);
        }
    }
}
