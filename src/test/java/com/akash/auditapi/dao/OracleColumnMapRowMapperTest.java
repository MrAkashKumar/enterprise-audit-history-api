package com.akash.auditapi.dao;

import org.junit.jupiter.api.Test;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OracleColumnMapRowMapperTest {
    private final OracleColumnMapRowMapper mapper = new OracleColumnMapRowMapper();

    @Test
    void convertsClobAndBlobIntoJsonSafeValues() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn(4L);
        when(clob.getSubString(1, 4)).thenReturn("text");
        when(resultSet.getObject(1)).thenReturn(clob);
        assertThat(mapper.getColumnValue(resultSet, 1)).isEqualTo("text");

        Blob blob = mock(Blob.class);
        byte[] bytes = {1, 2, 3};
        when(blob.length()).thenReturn(3L);
        when(blob.getBytes(1, 3)).thenReturn(bytes);
        when(resultSet.getObject(1)).thenReturn(blob);
        assertThat(mapper.getColumnValue(resultSet, 1))
                .isEqualTo(Base64.getEncoder().encodeToString(bytes));
    }
}
