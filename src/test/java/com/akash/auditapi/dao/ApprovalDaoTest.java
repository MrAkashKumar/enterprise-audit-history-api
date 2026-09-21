package com.akash.auditapi.dao;

import com.akash.auditapi.dto.ApprovalRecord;
import com.akash.auditapi.dto.ApprovalTableDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalDaoTest {
    private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    private final ApprovalDao dao = new ApprovalDao(jdbcTemplate, new ApprovalSqlBuilder());
    private final ApprovalTableDescriptor table = new ApprovalTableDescriptor(
            "PMC_CLIENT_APPROVAL", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");

    @Test
    void skipsDatabaseForEmptyIds() {
        assertThat(dao.findByIds(table, List.of())).isEmpty();
        verify(jdbcTemplate, never()).query(anyString(), any(MapSqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<ApprovalRecord>>any());
    }

    @Test
    void comparesApprovalIdAgainstThePagedEntityIds() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("ID")).thenReturn(10);
        when(resultSet.getString("MAKER_USERNAME")).thenReturn("maker.user");
        when(resultSet.getString("CHECKER_USERNAME")).thenReturn(null);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<ApprovalRecord>>any()))
                .thenAnswer(invocation -> {
                    assertThat(invocation.getArgument(0, String.class))
                            .contains("from PMC_CLIENT_APPROVAL where ID in (:ids)");
                    MapSqlParameterSource parameters = invocation.getArgument(1);
                    assertThat(parameters.getValue("ids")).isEqualTo(List.of(10));
                    RowMapper<ApprovalRecord> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        assertThat(dao.findByIds(table, List.of(10))).containsExactly(
                new ApprovalRecord(10, "maker.user", null));
    }
}
