package com.akash.auditapi.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RevisionOperationTest {
    @Test
    void mapsNumericStringAndUnexpectedValues() {
        assertThat(RevisionOperation.from(0)).isEqualTo(RevisionOperation.INSERT);
        assertThat(RevisionOperation.from("1")).isEqualTo(RevisionOperation.UPDATE);
        assertThat(RevisionOperation.from(2L)).isEqualTo(RevisionOperation.DELETE);
        assertThat(RevisionOperation.from("bad")).isEqualTo(RevisionOperation.UNKNOWN);
    }
}
