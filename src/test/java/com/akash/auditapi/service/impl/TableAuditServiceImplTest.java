package com.akash.auditapi.service.impl;

import com.akash.auditapi.dao.AuditIdPage;
import com.akash.auditapi.dao.ApprovalDao;
import com.akash.auditapi.dao.TableAuditDao;
import com.akash.auditapi.dto.response.AuditRevisionResponse;
import com.akash.auditapi.dto.ApprovalRecord;
import com.akash.auditapi.dto.ApprovalTableDescriptor;
import com.akash.auditapi.dto.ChangeSummary;
import com.akash.auditapi.enums.RevisionOperation;
import com.akash.auditapi.dto.response.SearchResponse;
import com.akash.auditapi.dto.TableDescriptor;
import com.akash.auditapi.resolver.AuditableTableCatalog;
import com.akash.auditapi.resolver.ApprovalTableResolver;
import com.akash.auditapi.resolver.EnversRevisionOperationResolver;
import com.akash.auditapi.resolver.TableDescriptorResolver;
import com.akash.auditapi.service.AuditHistoryAssembler;
import com.akash.auditapi.service.TableAuditService;
import com.akash.auditapi.validation.PaginationValidator;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TableAuditServiceImplTest {
    private final AuditableTableCatalog tableCatalog = mock(AuditableTableCatalog.class);
    private final TableDescriptorResolver tableResolver = mock(TableDescriptorResolver.class);
    private final TableAuditDao auditDao = mock(TableAuditDao.class);
    private final ApprovalDao approvalDao = mock(ApprovalDao.class);
    private final ApprovalTableResolver approvalTableResolver = mock(ApprovalTableResolver.class);
    private final PaginationValidator paginationValidator = mock(PaginationValidator.class);
    private final TableAuditService service = new TableAuditServiceImpl(
            tableCatalog, tableResolver, auditDao, approvalDao, approvalTableResolver,
            paginationValidator,
            new AuditHistoryAssembler(new EnversRevisionOperationResolver()));

    @Test void returnsDynamicTableLabels() {
        when(tableCatalog.labels()).thenReturn(List.of("Account-Statement", "Position-Balance"));

        assertThat(service.findAllTableLabels())
                .containsExactly("Account-Statement", "Position-Balance");
        verifyNoInteractions(tableResolver, auditDao, approvalDao,
                approvalTableResolver, paginationValidator);
    }

    @Test void groupsCurrentRowAndHistoryByIdIncludingDeletedRows() {
        TableDescriptor table = new TableDescriptor(
                "HOLIDAY_CALENDAR", "HOLIDAY_CALENDAR_AUD", "ID", "REV", "REVTYPE");
        when(tableCatalog.resolve("HOLIDAY_CALENDAR")).thenReturn("HOLIDAY_CALENDAR");
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
        ApprovalTableDescriptor approvalTable = new ApprovalTableDescriptor(
                "HOLIDAY_CALENDAR_APPROVAL", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");
        when(approvalTableResolver.resolve("HOLIDAY_CALENDAR"))
                .thenReturn(Optional.of(approvalTable));
        when(approvalDao.findByIds(approvalTable, List.of(1, 2))).thenReturn(List.of(
                new ApprovalRecord(new BigDecimal("1.00"), "user1", "vengza"),
                new ApprovalRecord(new BigDecimal("2.0"), "vengza", null)));
        SearchResponse response = service.sourceWithAuditHistory("HOLIDAY_CALENDAR", 0, 10);
        assertThat(response.getRows()).hasSize(2);
        assertThat(response.getRows().get(0).originalRecordPresent()).isTrue();
        assertThat(response.getRows().get(0).originalData()).isEqualTo(completeSourceRow);
        assertThat(response.getRows().get(0).auditHistory()).hasSize(2);
        assertThat(response.getRows().get(0).auditHistory()).extracting(AuditRevisionResponse::sequenceNumber)
                .containsExactly(1, 2);
        assertThat(response.getRows().get(0).auditHistory().get(0).data()).isEqualTo(completeAuditRow);
        assertThat(response.getRows().get(0).auditHistory().get(0).operation()).isEqualTo(RevisionOperation.INSERT);
        assertThat(response.getRows().get(0).changeSummary())
                .isEqualTo(new ChangeSummary(2, 1, 1, 0, 0, 10, 11));
        assertThat(response.getRows().get(0).approval().approvalRecordPresent()).isTrue();
        assertThat(response.getRows().get(0).approval().makerUsername()).isEqualTo("user1");
        assertThat(response.getRows().get(0).approval().checkerUsername()).isEqualTo("vengza");
        assertThat(response.getRows().get(1).originalRecordPresent()).isFalse();
        assertThat(response.getRows().get(1).originalData()).isNull();
        assertThat(response.getRows().get(1).changeSummary().deleteCount()).isEqualTo(1);
        assertThat(response.getRows().get(1).approval().approvalRecordPresent()).isTrue();
        assertThat(response.getRows().get(1).approval().makerUsername()).isEqualTo("vengza");
        assertThat(response.getRows().get(1).approval().checkerUsername()).isNull();
    }

    @Test void returnsEmptyPageWithoutQueryingRows() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        when(tableCatalog.resolve("POSITION_BALANCE")).thenReturn("POSITION_BALANCE");
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(List.of(), 0));

        SearchResponse response = service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10);

        assertThat(response.getRows()).isEmpty();
        assertThat(response.getTotalPages()).isZero();
        assertThat(response.isHasNext()).isFalse();
        verify(auditDao, never()).findSourceRows(table, List.of());
        verify(auditDao, never()).findAuditRows(table, List.of());
    }

    @Test void acceptsTheSamePublicLabelReturnedByAllTable() {
        TableDescriptor table = new TableDescriptor(
                "LOCO_SINGAPORE", "LOCO_SINGAPORE_AUD", "ID", "REV", "REVTYPE");
        when(tableCatalog.resolve("Loco Singapore")).thenReturn("LOCO_SINGAPORE");
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
        when(tableCatalog.resolve("POSITION_BALANCE")).thenReturn("POSITION_BALANCE");
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
        List<Object> ids = List.of("A1", 2);
        when(tableCatalog.resolve("POSITION_BALANCE")).thenReturn("POSITION_BALANCE");
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 2));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(
                Map.of("ID", "A1", "STATUS", "CURRENT")));
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
        when(tableCatalog.resolve("POSITION_BALANCE")).thenReturn("POSITION_BALANCE");
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
        when(tableCatalog.resolve("POSITION_BALANCE")).thenReturn("POSITION_BALANCE");
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 1));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(Map.of("STATUS", "INVALID")));

        assertThatThrownBy(() -> service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database row contains a null entity ID");
    }

    @Test void preservesAuditResponseWhenApprovalRowsAreDuplicated() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        ApprovalTableDescriptor approvalTable = new ApprovalTableDescriptor(
                "POSITION_BALANCE_APPROVAL", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");
        List<Object> ids = List.of(1);
        when(tableCatalog.resolve("POSITION_BALANCE")).thenReturn("POSITION_BALANCE");
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 1));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(Map.of("ID", 1)));
        when(auditDao.findAuditRows(table, ids)).thenReturn(List.of());
        when(approvalTableResolver.resolve("POSITION_BALANCE"))
                .thenReturn(Optional.of(approvalTable));
        when(approvalDao.findByIds(approvalTable, ids)).thenReturn(List.of(
                new ApprovalRecord(1, "maker.one", null),
                new ApprovalRecord(1, "maker.two", "checker")));

        SearchResponse response = service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10);

        assertThat(response.getRows().getFirst().originalRecordPresent()).isTrue();
        assertThat(response.getRows().getFirst().approval()).isEqualTo(
                com.akash.auditapi.dto.response.ApprovalResponse.ABSENT);
    }

    @Test void matchesStringApprovalIdWithoutChangingSourceAndAuditGrouping() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        ApprovalTableDescriptor approvalTable = new ApprovalTableDescriptor(
                "POSITION_BALANCE_APPROVAL", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");
        List<Object> ids = List.of("A1");
        when(tableCatalog.resolve("POSITION_BALANCE")).thenReturn("POSITION_BALANCE");
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 1));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(Map.of("ID", "A1")));
        when(auditDao.findAuditRows(table, ids)).thenReturn(List.of());
        when(approvalTableResolver.resolve("POSITION_BALANCE"))
                .thenReturn(Optional.of(approvalTable));
        when(approvalDao.findByIds(approvalTable, ids)).thenReturn(List.of(
                new ApprovalRecord("A1", "maker.user", null)));

        SearchResponse response = service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10);

        assertThat(response.getRows().getFirst().originalRecordPresent()).isTrue();
        assertThat(response.getRows().getFirst().approval().approvalRecordPresent()).isTrue();
        assertThat(response.getRows().getFirst().approval().makerUsername()).isEqualTo("maker.user");
    }

    @Test void preservesAuditResponseWhenAnApprovalRowHasNoId() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        ApprovalTableDescriptor approvalTable = new ApprovalTableDescriptor(
                "POSITION_BALANCE_APPROVAL", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");
        List<Object> ids = List.of(1);
        when(tableCatalog.resolve("POSITION_BALANCE")).thenReturn("POSITION_BALANCE");
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 1));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(Map.of("ID", 1)));
        when(auditDao.findAuditRows(table, ids)).thenReturn(List.of());
        when(approvalTableResolver.resolve("POSITION_BALANCE"))
                .thenReturn(Optional.of(approvalTable));
        when(approvalDao.findByIds(approvalTable, ids)).thenReturn(List.of(
                new ApprovalRecord(null, "maker.user", null)));

        SearchResponse response = service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10);

        assertThat(response.getRows().getFirst().originalRecordPresent()).isTrue();
        assertThat(response.getRows().getFirst().approval()).isEqualTo(
                com.akash.auditapi.dto.response.ApprovalResponse.ABSENT);
    }

    @Test void preservesAuditResponseWhenApprovalQueryFails() {
        TableDescriptor table = new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");
        ApprovalTableDescriptor approvalTable = new ApprovalTableDescriptor(
                "POSITION_BALANCE_APPROVAL", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");
        List<Object> ids = List.of(1);
        when(tableCatalog.resolve("POSITION_BALANCE")).thenReturn("POSITION_BALANCE");
        when(tableResolver.resolve("POSITION_BALANCE")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 1));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(Map.of("ID", 1)));
        when(auditDao.findAuditRows(table, ids)).thenReturn(List.of(Map.of(
                "ID", 1, "REV", 10, "REVTYPE", 0)));
        when(approvalTableResolver.resolve("POSITION_BALANCE"))
                .thenReturn(Optional.of(approvalTable));
        when(approvalDao.findByIds(approvalTable, ids))
                .thenThrow(new DataRetrievalFailureException("approval unavailable"));

        SearchResponse response = service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10);

        assertThat(response.getRows().getFirst().originalRecordPresent()).isTrue();
        assertThat(response.getRows().getFirst().auditHistory()).hasSize(1);
        assertThat(response.getRows().getFirst().approval()).isEqualTo(
                com.akash.auditapi.dto.response.ApprovalResponse.ABSENT);
    }

    @Test void preservesIndependentlyNullableApprovalUsernames() {
        TableDescriptor table = new TableDescriptor(
                "PMC_CLIENT", "PMC_CLIENT_AUD", "ID", "REV", "REVTYPE");
        ApprovalTableDescriptor approvalTable = new ApprovalTableDescriptor(
                "PMC_CLIENT_APPROVAL", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");
        List<Object> ids = List.of(1, 2);
        when(tableCatalog.resolve("PMC_CLIENT")).thenReturn("PMC_CLIENT");
        when(tableResolver.resolve("PMC_CLIENT")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 2));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(
                Map.of("ID", 1, "CLIENT_NAME", "One"),
                Map.of("ID", 2, "CLIENT_NAME", "Two")));
        when(auditDao.findAuditRows(table, ids)).thenReturn(List.of());
        when(approvalTableResolver.resolve("PMC_CLIENT"))
                .thenReturn(Optional.of(approvalTable));
        when(approvalDao.findByIds(approvalTable, ids)).thenReturn(List.of(
                new ApprovalRecord(1, null, "checker.user"),
                new ApprovalRecord(2, null, null)));

        SearchResponse response = service.sourceWithAuditHistory("PMC_CLIENT", 0, 10);

        assertThat(response.getRows().get(0).approval().approvalRecordPresent()).isTrue();
        assertThat(response.getRows().get(0).approval().makerUsername()).isNull();
        assertThat(response.getRows().get(0).approval().checkerUsername())
                .isEqualTo("checker.user");
        assertThat(response.getRows().get(1).approval().approvalRecordPresent()).isTrue();
        assertThat(response.getRows().get(1).approval().makerUsername()).isNull();
        assertThat(response.getRows().get(1).approval().checkerUsername()).isNull();
    }

    @Test void returnsCompleteApprovalTableAndAuditWhenSelectedDirectly() {
        TableDescriptor table = new TableDescriptor(
                "PMC_CLIENT_APPROVAL", "PMC_CLIENT_APPROVAL_AUD", "ID", "REV", "REVTYPE");
        List<Object> ids = List.of(11);
        Map<String, Object> sourceRow = Map.of(
                "ID", 11, "MAKER_USERNAME", "maker.user", "CHECKER_USERNAME", "checker.user",
                "STATUS", "APPROVED", "COMMENTS", "Complete current data");
        Map<String, Object> auditRow = Map.of(
                "ID", 11, "REV", 901, "REVTYPE", 0,
                "MAKER_USERNAME", "maker.user", "CHECKER_USERNAME", "checker.user",
                "STATUS", "PENDING", "COMMENTS", "Complete historical data");
        when(tableCatalog.resolve("PMC_CLIENT_APPROVAL")).thenReturn("PMC_CLIENT_APPROVAL");
        when(tableResolver.resolve("PMC_CLIENT_APPROVAL")).thenReturn(table);
        when(auditDao.findIdPage(table, 0, 10)).thenReturn(new AuditIdPage(ids, 1));
        when(auditDao.findSourceRows(table, ids)).thenReturn(List.of(sourceRow));
        when(auditDao.findAuditRows(table, ids)).thenReturn(List.of(auditRow));
        when(approvalTableResolver.resolve("PMC_CLIENT_APPROVAL")).thenReturn(Optional.empty());

        SearchResponse response = service.sourceWithAuditHistory("PMC_CLIENT_APPROVAL", 0, 10);

        assertThat(response.getRows().getFirst().originalData()).isEqualTo(sourceRow);
        assertThat(response.getRows().getFirst().auditHistory().getFirst().data())
                .isEqualTo(auditRow);
        assertThat(response.getRows().getFirst().approval()).isEqualTo(
                com.akash.auditapi.dto.response.ApprovalResponse.ABSENT);
        verify(approvalTableResolver).resolve("PMC_CLIENT_APPROVAL");
        verifyNoInteractions(approvalDao);
    }

}
