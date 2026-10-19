package com.example.indexreviewer.model.audit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FlatCapTest {

    private final FlatCap smiCap = new FlatCap(new BigDecimal("18"));

    @Test
    void findLimit_whenAnyPosition_expectSameCap() {
        BigDecimal largest = smiCap.findLimit(0);
        BigDecimal smallest = smiCap.findLimit(19);

        assertThat(largest).isEqualByComparingTo("18");
        assertThat(smallest).isEqualByComparingTo("18");
    }
}
