package com.example.indexreviewer.model.audit;

import lombok.Value;

import java.math.BigDecimal;

/**
 * The same maximum weight for every index component, such as 18% for the SMI (rulebook section 5.12.4).
 */
@Value
public class FlatCap implements CappingRule {

    BigDecimal cap;

    @Override
    public BigDecimal findLimit(int position) {
        return cap;
    }
}
