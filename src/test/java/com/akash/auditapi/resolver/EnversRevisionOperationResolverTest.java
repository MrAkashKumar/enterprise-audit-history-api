package com.akash.auditapi.resolver;

import com.akash.auditapi.enums.RevisionOperation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EnversRevisionOperationResolverTest {
    private final RevisionOperationResolver resolver = new EnversRevisionOperationResolver();

    @Test
    void resolvesAllEnversTypesAndUnknownValues() {
        assertThat(resolver.resolve(BigDecimal.ZERO)).isEqualTo(RevisionOperation.INSERT);
        assertThat(resolver.resolve((short) 1)).isEqualTo(RevisionOperation.UPDATE);
        assertThat(resolver.resolve("2")).isEqualTo(RevisionOperation.DELETE);
        assertThat(resolver.resolve(null)).isEqualTo(RevisionOperation.UNKNOWN);
        assertThat(resolver.resolve(9)).isEqualTo(RevisionOperation.UNKNOWN);
    }
}
