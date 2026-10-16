package com.example.indexreviewer.config;

import com.example.indexreviewer.dto.request.ConfigRequest;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Config/Fix to handle Swagger UI requests. Reads the JSON {@code configRequest} multipart when the client sends it
 * without a Content-Type, which Swagger UI and plain curl do. Such parts arrive as application/octet-stream.
 */
public class ConfigRequestPartConverter extends JacksonJsonHttpMessageConverter {

    public ConfigRequestPartConverter(JsonMapper jsonMapper) {
        super(jsonMapper);
        setSupportedMediaTypes(List.of(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Override
    public boolean canRead(ResolvableType type, MediaType mediaType) {
        return ConfigRequest.class.equals(type.resolve()) && super.canRead(type, mediaType);
    }

    @Override
    public boolean canWrite(ResolvableType type, Class<?> valueClass, MediaType mediaType) {
        return false;
    }
}
