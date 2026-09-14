package com.akash.auditapi.service;

import com.akash.auditapi.dao.AuditIdPage;
import com.akash.auditapi.dao.TableAuditDao;
import com.akash.auditapi.model.AuditRevisionResponse;
import com.akash.auditapi.model.ChangeSummary;
import com.akash.auditapi.model.RevisionOperation;
import com.akash.auditapi.model.SearchResponse;
import com.akash.auditapi.model.TableDescriptor;
import com.akash.auditapi.resolver.EnversRevisionOperationResolver;
import com.akash.auditapi.resolver.TableDescriptorResolver;
import com.akash.auditapi.validation.PaginationValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TableAuditServiceTest {
    private final TableDescriptorResolver tableResolver = mock(TableDescriptorResolver.class);
    private final TableAuditDao auditDao = mock(TableAuditDao.class);
    private final PaginationValidator paginationValidator = mock(PaginationValidator.class);
    private final TableAuditService service = new TableAuditService(tableResolver, auditDao,
            paginationValidator,
            new AuditHistoryAssembler(new EnversRevisionOperationResolver()));

    @Test void groupsCurrentRowAndHistoryByIdIncludingDeletedRows() {
        TableDescriptor table = new TableDescriptor(
                "HOLIDAY_CALENDAR", "HOLIDAY_CALENDAR_AUD", "ID", "REV", "REVTYPE");
        when(tableResolver.resolve("HOLIDAY_CALENDAR")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(List.of(1, 2), 2));
        Map<String, Object> completeSourceRow = Map.of(
                "ID", 1,
                "HOLIDAY_DATE", "2026-01-01",
                "HOLIDAY_NAME", "Current",
                "ACTIVE", 1,
                "UPDATED_AT", "2026-01-01T10:00:00Z");
        Map<String, Object> completeAuditRow = Map.of(
                "REV", 10,
                "REVTYPE", 0,
                "ID", 1,
                "HOLIDAY_DATE", "2026-01-01",
                "HOLIDAY_NAME", "Original",
                "ACTIVE", 1,
                "UPDATED_AT", "2026-01-01T00:00:00Z",
                "AUDIT_USER", "AUDIT_APP");
        when(auditDao.findSourceRows(table, List.of(1, 2))).thenReturn(List.of(completeSourceRow));
        when(auditDao.findAuditRows(table, List.of(1, 2))).thenReturn(List.of(
                completeAuditRow, Map.of("ID", 1, "REV", 11, "REVTYPE", 1),
                Map.of("ID", 2, "REV", 12, "REVTYPE", 2)));
        SearchResponse response = service.sourceWithAuditHistory("HOLIDAY_CALENDAR", 0, 10);
        assertThat(response.getRows()).hasSize(2);
        assertThat(response.getRows().get(0).originalRecordPresent()).isTrue();
        assertThat(response.getRows().get(0).originalData()).containsEntry("HOLIDAY_NAME", "Current");
        assertThat(response.getRows().get(0).originalData()).isEqualTo(completeSourceRow);
        assertThat(response.getRows().get(0).auditHistory()).hasSize(2);
        assertThat(response.getRows().get(0).auditHistory()).extracting(AuditRevisionResponse::sequenceNumber)
                .containsExactly(1, 2);
        assertThat(response.getRows().get(0).auditHistory().get(0).data()).isEqualTo(completeAuditRow);
        assertThat(response.getRows().get(0).auditHistory().get(0).operation()).isEqualTo(RevisionOperation.INSERT);
        assertThat(response.getRows().get(0).changeSummary())
                .isEqualTo(new ChangeSummary(2, 1, 1, 0, 0, 10, 11));
        assertThat(response.getRows().get(1).originalRecordPresent()).isFalse();
        assertThat(response.getRows().get(1).originalData()).isNull();
        assertThat(response.getRows().get(1).changeSummary().deleteCount()).isEqualTo(1);
    }

    @Test void returnsEmptyPageWithoutQueryingRows() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(List.of(), 0));

        SearchResponse response = service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10);

        assertThat(response.getRows()).isEmpty();
        assertThat(response.getTotalPages()).isZero();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test void acceptsTheSamePublicLabelReturnedByAllTable() {
        TableDescriptor table = new TableDescriptor(
                "LOCO_SINGAPORE", "LOCO_SINGAPORE_AUD", "ID", "REV", "REVTYPE");
        when(tableResolver.resolve("LOCO_SINGAPORE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(List.of(), 0));

        SearchResponse response = service.sourceWithAuditHistory("Loco Singapore", 0, 10);

        assertThat(response.getRows()).isEmpty();
        verify(tableResolver).resolve("LOCO_SINGAPORE");
    }

    @Test void keepsGlobalEnversRevisionSequenceIndependentForEachEntity() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        List<Object> ids = List.of(1002, 1003);
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 2));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(
                Map.of("ID", 1002, "TOTAL_AGGREGATED_QUANTITY", 0),
                Map.of("ID", 1003, "TOTAL_AGGREGATED_QUANTITY", 801)));
        when(auditDao.findAuditRows(table, ids)).thenReturn(List.of(
                Map.of("ID", 1002, "REV", 9063, "REVTYPE", 0, "TOTAL_AGGREGATED_QUANTITY", 801),
                Map.of("ID", 1002, "REV", 9071, "REVTYPE", 1, "TOTAL_AGGREGATED_QUANTITY", 0),
                Map.of("ID", 1003, "REV", 9071, "REVTYPE", 0, "TOTAL_AGGREGATED_QUANTITY", 801)));

        SearchResponse response = service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10);

        assertThat(response.getNumberOfElements()).isEqualTo(2);
        assertThat(response.getRows().get(0).auditHistory())
                .extracting(AuditRevisionResponse::sequenceNumber, AuditRevisionResponse::revision)
                .containsExactly(tuple(1, 9063), tuple(2, 9071));
        assertThat(response.getRows().get(1).auditHistory())
                .extracting(AuditRevisionResponse::sequenceNumber, AuditRevisionResponse::revision)
                .containsExactly(tuple(1, 9071));
    }

    @Test void handlesSourceOnlyRowsAndUnknownRevisionTypes() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        List<Object> ids = List.of(1, 2);
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 2));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(
                Map.of("ID", 1, "STATUS", "CURRENT")));
        when(auditDao.findAuditRows(table, ids)).thenReturn(List.of(
                Map.of("ID", 2, "REV", 50, "REVTYPE", 99)));

        SearchResponse response = service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10);

        assertThat(response.getRows().get(0).changeSummary()).isEqualTo(ChangeSummary.EMPTY);
        assertThat(response.getRows().get(0).auditHistory()).isEmpty();
        assertThat(response.getRows().get(1).changeSummary().unknownCount()).isEqualTo(1);
        assertThat(response.getRows().get(1).auditHistory().getFirst().operation())
                .isEqualTo(RevisionOperation.UNKNOWN);
    }

    @Test void calculatesPreviousAndNextForMiddlePages() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 1, 10)).thenReturn(new AuditIdPage(List.of(), 30));

        SearchResponse response = service.sourceWithAuditHistory("POSITION_BALANCE", 1, 10);

        assertThat(response.isHasPrevious()).isTrue();
        assertThat(response.isHasNext()).isTrue();
    }

    @Test void rejectsDatabaseRowsWithoutTheConfiguredEntityId() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        List<Object> ids = List.of(1);
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 1));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(Map.of("STATUS", "INVALID")));

        assertThatThrownBy(() -> service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database row contains a null entity ID");
    }

}
