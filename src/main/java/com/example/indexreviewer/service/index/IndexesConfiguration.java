package com.example.indexreviewer.service.index;

import com.example.indexreviewer.exception.IndexNotConfiguredException;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.audit.IndexRules;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Rules of every configured index, bound from the indices section of application.yml and checked at startup.
 */
@Validated
@ConfigurationProperties
public record IndexesConfiguration(@NotEmpty Map<IndexType, @Valid IndexConfiguration> indices) {

    public IndexRules findRules(IndexType indexType) {
        IndexConfiguration configuration = indices.get(indexType);
        if (configuration == null) {
            throw new IndexNotConfiguredException(indexType);
        }
        return configuration.toRules();
    }
}
