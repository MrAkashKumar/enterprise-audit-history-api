package com.akash.auditapi.dto.response;

import com.akash.auditapi.enums.RevisionOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        databaseColumns.put("ID", 9999);
        assertThat(response.databaseColumns()).containsEntry("ID", 1002);
        assertThatThrownBy(() -> response.databaseColumns().put("ID", 9999))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
