package com.toletter.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;

@Converter
public class LongListConverter implements AttributeConverter<List<Long>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Long> dataInfo) {
        try {
            return objectMapper.writeValueAsString(dataInfo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 오류: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Long> convertToEntityAttribute(String data) {
        try {
            return objectMapper.readValue(data, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 역직렬화 오류: " + e.getMessage(), e);
        }
    }
}
