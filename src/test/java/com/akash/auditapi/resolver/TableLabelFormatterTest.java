package com.akash.auditapi.resolver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TableLabelFormatterTest {
    private final TableLabelFormatter formatter = new TableLabelFormatter();

    @Test
    void removesPrefixCapitalizesWordsAndJoinsWithHyphens() {
        assertThat(formatter.format("PMC_ACCOUNT_STATEMENT", "PMC_"))
                .isEqualTo("Account-Statement");
        assertThat(formatter.format("PMC_LOCO_SINGAPORE", "PMC_"))
                .isEqualTo("Loco-Singapore");
    }
}
