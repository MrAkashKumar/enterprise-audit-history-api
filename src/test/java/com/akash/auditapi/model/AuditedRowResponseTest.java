package com.akash.auditapi.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditedRowResponseTest {
    @Test
    void snapshotsNullableDatabaseRowsAndHistory() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("ID", 1002);
        source.put("DESCRIPTION", null);
        List<AuditRevisionResponse> history = new ArrayList<>();
        history.add(new AuditRevisionResponse(1, 10, 0, RevisionOperation.INSERT,
                Map.of("ID", 1002, "REV", 10, "REVTYPE", 0)));

        AuditedRowResponse response = new AuditedRowResponse(
                1002, true, source, new ChangeSummary(1, 1, 0, 0, 0, 10, 10), history);
        source.put("ID", 9999);
        history.clear();

        assertThat(response.originalData()).containsEntry("ID", 1002)
                .containsEntry("DESCRIPTION", null);
        assertThat(response.auditHistory()).hasSize(1);
        assertThatThrownBy(() -> response.originalData().put("ID", 9999))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
