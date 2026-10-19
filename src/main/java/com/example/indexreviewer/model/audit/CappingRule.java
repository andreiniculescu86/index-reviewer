package com.example.indexreviewer.model.audit;

import java.math.BigDecimal;

/**
 * Maximum weight of each index component, as a percentage of the index, 18 for 18%. The components are counted in rank
 * order, so a rule can give the largest components a different limit than the others.
 */
public sealed interface CappingRule permits FlatCap {

    /**
     * Returns the maximum weight of the component at the given position in rank order, 0 for the largest.
     */
    BigDecimal findLimit(int position);
}
