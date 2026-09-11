package com.akash.auditapi.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRevisionResponseTest {
    @Test
    void flattensAllDatabaseColumnsBesideReadableOperation() throws Exception {
        Map<String, Object> databaseColumns = new LinkedHashMap<>();
        databaseColumns.put("ID", 1002);
        databaseColumns.put("REV", 9071);
        databaseColumns.put("REVTYPE", 1);
        databaseColumns.put("TOTAL_AGGREGATED_QUANTITY", 0);
        databaseColumns.put("AUDIT_USER", "AUDIT_APP");
        databaseColumns.put("CREATED_BY", null);
        AuditRevisionResponse response = new AuditRevisionResponse(2, 9071, 1,
                RevisionOperation.UPDATE, databaseColumns);

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"sequenceNumber\":2", "\"operation\":\"UPDATE\"",
                        "\"ID\":1002", "\"TOTAL_AGGREGATED_QUANTITY\":0",
                        "\"AUDIT_USER\":\"AUDIT_APP\"", "\"CREATED_BY\":null")
                .doesNotContain("\"data\"");
    }
}
