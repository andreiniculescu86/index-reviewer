package com.example.indexreviewer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Index Reviewer API", version = "v1",
        description = "Runs rule-based index reviews, starting with the SMI review for Q3 2026, "
                + "and exposes their results, traces and audit records."))
public class OpenApiConfig {
}
