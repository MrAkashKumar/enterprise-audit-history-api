package com.akash.auditapi.dao;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.stereotype.Component;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.Base64;

import static com.akash.auditapi.exception.ApiMessages.LOB_TOO_LARGE;

@Component
public class OracleColumnMapRowMapper extends ColumnMapRowMapper {
    @Override
    protected Object getColumnValue(ResultSet resultSet, int index) throws SQLException {
        Object value = super.getColumnValue(resultSet, index);
        try {
            if (value instanceof Clob clob) {
                return clob.getSubString(1, Math.toIntExact(clob.length()));
            }
            if (value instanceof Blob blob) {
                return Base64.getEncoder().encodeToString(blob.getBytes(1, Math.toIntExact(blob.length())));
            }
            if (value instanceof byte[] bytes) {
                return Base64.getEncoder().encodeToString(bytes);
            }
            if (value instanceof SQLXML sqlxml) {
                return sqlxml.getString();
            }
            return value;
        } catch (ArithmeticException exception) {
            throw new DataRetrievalFailureException(LOB_TOO_LARGE, exception);
        }
    }
}
