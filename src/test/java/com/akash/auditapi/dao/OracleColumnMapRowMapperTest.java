package com.akash.auditapi.dao;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLXML;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.akash.auditapi.exception.ApiMessages.LOB_TOO_LARGE;
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

    @Test
    void convertsBytesAndSqlXmlAndDelegatesOrdinaryValues() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        byte[] bytes = {4, 5, 6};
        when(resultSet.getObject(1)).thenReturn(bytes);
        assertThat(mapper.getColumnValue(resultSet, 1))
                .isEqualTo(Base64.getEncoder().encodeToString(bytes));

        SQLXML sqlxml = mock(SQLXML.class);
        when(sqlxml.getString()).thenReturn("<value/>");
        when(resultSet.getObject(1)).thenReturn(sqlxml);
        assertThat(mapper.getColumnValue(resultSet, 1)).isEqualTo("<value/>");

        when(resultSet.getObject(1)).thenReturn("ordinary");
        assertThat(mapper.getColumnValue(resultSet, 1)).isEqualTo("ordinary");
    }

    @Test
    void rejectsLobsLargerThanTheSupportedArraySize() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        Blob blob = mock(Blob.class);
        when(blob.length()).thenReturn((long) Integer.MAX_VALUE + 1);
        when(resultSet.getObject(1)).thenReturn(blob);

        assertThatThrownBy(() -> mapper.getColumnValue(resultSet, 1))
                .isInstanceOf(DataRetrievalFailureException.class)
                .hasMessage(LOB_TOO_LARGE);
    }
}
