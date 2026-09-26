# Product Requirements Document: Enterprise Audit History API

## 1. Purpose

Provide a reusable Java 21 and Spring Boot REST API that returns current rows from a discovered
Oracle source table and complete Hibernate Envers history when `<TABLE>_AUD` exists. The generic
endpoint must support audited and source-only tables without table-specific controllers, entities,
repositories, or response models.

## 2. Users and use cases

- Operations and support teams investigating insert, update, and delete activity.
- Internal services retrieving current records and historical snapshots together.
- Engineers validating Oracle and Envers audit behavior.
- Administrators retrieving the user-facing list of supported source tables.

## 3. API response standard

Every controller response uses one JSON envelope. Domain content is returned only under `data`.

| Field | Type | Success | Error | Meaning |
|---|---|---:|---:|---|
| `timestamp` | ISO-8601 string | Yes | Yes | Time the response was created |
| `status` | string | `SUCCESS` | Specific failure status | Outcome matching the response message |
| `code` | four-digit string | `2000` | `4xxx`/`5xxx` | Stable application response code |
| `message` | string | Yes | Yes | Safe user-readable result message |
| `data` | object/null | Payload | `null` | Endpoint-specific response DTO |
| `error` | string | No | Yes | Standard HTTP reason phrase |
| `details` | array | No | Yes | Validation failures; empty for non-validation errors |

Success envelope:

```json
{
  "timestamp": "<ISO-8601 timestamp>",
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": {}
}
```

Error envelope:

```json
{
  "timestamp": "<ISO-8601 timestamp>",
  "status": "INTERNAL_ERROR",
  "code": "5000",
  "message": "An unexpected error occurred",
  "data": null,
  "error": "Internal Server Error",
  "details": []
}
```

`path`, the numeric HTTP status, SQL, credentials, stack traces, and Oracle messages are never
returned. One `ApiOutcomeCode` enum defines each specific outcome and application code. Clients
branch on `status` and `code`; the protocol status remains available from the HTTP response.

The implementation uses `BaseApiResponse` for shared metadata, `ApiResponse<T>` for successful
payloads, and `ApiError` for failures. This avoids metadata duplication while keeping the envelope
flat and consistent.

The canonical examples for every endpoint and error outcome are maintained in
[API_RESPONSE_EXAMPLES.md](API_RESPONSE_EXAMPLES.md). Implemented JSON must remain consistent with
that reference.

## 4. Endpoints and request contracts

### 4.1 Source row with audit history

```http
GET /api/v1/{tableName}?pageNo=0&pageSize=10
```

This GET endpoint has no request body.

| Input | Location | Required | Rules |
|---|---|---:|---|
| `tableName` | Path | Yes | Discovered source name or returned label; audit names are rejected |
| `pageNo` | Query | No | Zero-based; default `0`; must be at least `0` |
| `pageSize` | Query | No | Default `10`; range `1..audit-api.max-page-size` |

`data` is a `SearchResponse` with:

| Field | Type | Meaning |
|---|---|---|
| `pageNo`, `pageSize` | integer | Applied pagination |
| `numberOfElements` | integer | IDs returned on this page |
| `totalElements`, `totalPages` | integer | Distinct-ID totals |
| `hasPrevious`, `hasNext` | boolean | Page navigation state |
| `rows` | array | Records grouped by entity ID |

Each item in `rows` contains `id`, `originalRecordPresent`, `originalData`, `approval`,
`changeSummary`, and `auditHistory`. `originalData` includes every source column returned by Oracle. Each history item
contains the derived sequence/revision/operation fields and every audit-table column.

The row contract is state-independent:

| Entity state | Required API representation |
|---|---|
| Current source row exists | `originalRecordPresent=true`; `originalData` is the complete current row |
| Source row was deleted | `originalRecordPresent=false`; `originalData=null`; history remains complete |
| No audit revisions exist | Zero-valued `changeSummary`; empty `auditHistory` |
| Unknown `REVTYPE` exists | Preserve the row and return `operation=UNKNOWN`; increment `unknownCount` |

`approval` is always present and contains `approvalRecordPresent`, `makerUsername`, and
`checkerUsername`. Missing approval tables or rows return `false` and JSON `null` usernames.
Approval rows are matched directly using the same numeric `ID`, including for deleted source rows.
Approval ID matching is normalized without scale so Oracle values such as `1`, `1.0`, and `1.00`
match the same response entity. Existing source/audit grouping remains unchanged.
`approvalRecordPresent` is based on row existence, not status or checker nullability:
an `APPROVED` row normally has maker/checker, while a `PENDING` row remains present with a nullable
checker.
The physical maker column is optional. When it is absent but a row with the required `ID` and
`CHECKER_USERNAME` columns exists, return `approvalRecordPresent=true`, `makerUsername=null`, and
the database checker value.

The service must preserve database `null` values and source column order in map-backed snapshots.
It must not replace full row data with a reduced, table-specific projection. The success response
must not contain `sourceTable`, `auditTable`, `path`, or `processingTimeMs`. Source and
audit table names remain internal query metadata.

### 4.2 Supported table labels

```http
GET /api/v1/allTable
```

No request body or pagination is accepted. The API retains its original behavior: it queries Oracle
`USER_TABLES` and selects every non-audit name with the configured prefix (default `PMC_`). It
returns case-insensitively alphabetized labels under
`data.tableLabels`. For example,
`PMC_ACCOUNT_STATEMENT` becomes `Account-Statement`. Audit table names are not exposed.
This endpoint must remain independent of approval metadata resolution and approval-row queries.
Approval enrichment is an additive responsibility of the detail endpoint only.
The detail endpoint separately requires the selected table's exact audit companion.

### 4.3 Holiday CRUD example

```http
GET    /api/v1/holidays?pageNo=0&pageSize=10
POST   /api/v1/holidays
PUT    /api/v1/holidays/{id}
DELETE /api/v1/holidays/{id}
```

GET and DELETE have no request body. POST and PUT accept `HolidayRequest`:

| Field | Type | Validation |
|---|---|---|
| `id` | positive integer | Required and positive |
| `holidayDate` | ISO local date | Required |
| `calendarCode` | string | Required, non-blank, maximum 50 characters |
| `calendarName` | string | Required, non-blank, maximum 200 characters |
| `username` | string | Required, non-blank, maximum 128 characters |

Responses never expose the JPA entity directly. POST/PUT return `HolidayResponse` under `data`.
The paginated GET returns `PageResponse<HolidayResponse>`. DELETE returns the success envelope with
`data: null`. POST uses HTTP `201`; other successful operations use HTTP `200`.

### 4.4 Commodity debit PDF download

```http
GET /api/v1/reports/commodity-debits/{id}/pdf
```

The request has no body. The service reads the row identified by numeric `ID` from
`PMC_COMMODITY_DEBIT` through JPA and renders an A4 portrait PDF using iText. The successful response
uses `Content-Type: application/pdf`, disables caching, and downloads with the exact filename
`community.pdf`. Binary success does not use the JSON success envelope; every failure still uses
the standard JSON error envelope.

The visual design follows the supplied SWIFT MT606 reference: incoming/customer-print headers,
reference and routing blocks, fields `:20:`, `:21:`, `:26C:`, `:25:`, `:30:`, `:32F:`, `:87A:`,
`:88D:`, an end-message separator, and the database-provided printed timestamp. Null database values
render as blank text. The mapped Oracle columns are `ID`, `INCOMING_REFERENCE`,
`TRANSACTION_REFERENCE`, `MESSAGE_TYPE`, `SENDER_BIC`, `SENDER_NAME`, `SENDER_LOCATION`,
`RECEIVER_BIC`, `RECEIVER_NAME`, `RECEIVER_LOCATION`, `NETWORK_CHANNEL`, `NETWORK_REFERENCE`,
`DATA_OWNER`, `PHASE_ACTION`, `MUR`, `RELATED_REFERENCE`, `DELIVERY_LOCATION`, `ALLOCATION`,
`COMMODITY_TYPE`, `ACCOUNT_IDENTIFICATION`, `VALUE_DATE`, `COMMODITY_UNIT`, `COMMODITY_AMOUNT`,
`COMMODITY_RECEIVER_IDENTIFIER`, `COMMODITY_RECEIVER_NAME`, `COMMODITY_RECEIVER_ADDRESS`,
`BENEFICIARY_NAME`, `BENEFICIARY_ADDRESS`, and `PRINTED_ON`.

## 5. Audit behavior and data semantics

### 5.1 Recommended Oracle architecture

The production auditing model is **row-level trigger + audit table**. Oracle is the system of
record for audit capture. Each audited source table has:

- A corresponding `<SOURCE_TABLE>_AUD` history table.
- An `AFTER INSERT OR UPDATE OR DELETE FOR EACH ROW` trigger managed by the database/DBA platform.
- A shared entity key (`ID`) and Envers-compatible `REV` and `REVTYPE` audit columns.
- Full source-column snapshots plus any approved audit metadata columns.

Trigger behavior is:

| Source DML | Snapshot written | `REVTYPE` | API operation |
|---|---|---:|---|
| `INSERT` | `:NEW` row | `0` | `INSERT` |
| `UPDATE` | Updated `:NEW` row | `1` | `UPDATE` |
| `DELETE` | Final `:OLD` row | `2` | `DELETE` |

The trigger's audit insert must normally execute in the source transaction, without an autonomous
commit, so source and history changes commit or roll back atomically. Revision numbers are supplied
by the centralized Oracle audit mechanism and must be monotonically orderable. If revisions are
transaction-wide, the database implementation must allocate/reuse one revision consistently for
all affected rows rather than relying on an independent row-trigger sequence call.

Audit history is append-only. Runtime application accounts receive no update/delete privileges on
audit tables. Trigger code, revision allocation, deployment, reconciliation, retention,
partitioning, archival, and optional actor/timestamp capture remain database/DBA responsibilities.
This API does not create triggers, generate revisions, or insert audit records.

### 5.2 API read behavior

1. Resolve the optional audit table using `<SOURCE_TABLE>_AUD`.
2. Require `ID` on the source. When the audit table exists, require `ID`, `REV`, and `REVTYPE`;
   never hide malformed audit metadata by falling back to source-only mode.
3. Page distinct non-null IDs across the union of source and audit tables. When no audit table
   exists, page IDs from the source table only.
4. Include audit-only IDs so deleted entities remain discoverable.
5. Load complete source rows and, when available, complete audit snapshots using `SELECT *`.
6. Group source and history rows using the existing ID representation without changing the
   original source/audit behavior.
7. Sort audit history by `REV` ascending and assign a one-based `sequenceNumber` per entity.
8. Map `REVTYPE`: `0=INSERT`, `1=UPDATE`, `2=DELETE`; other values become `UNKNOWN`.
9. Calculate total, insert, update, delete, and unknown counts plus first/latest revisions.
10. For source-only tables, return an empty `auditHistory` and zero-valued `changeSummary`.
11. Resolve an optional `<SOURCE>_APPROVAL_REQUEST` or `<SOURCE>_APPROVAL` table and load checker,
    plus maker when that physical column exists, for every page ID in one query.

`REV` is a global transaction revision and can occur for multiple IDs. `sequenceNumber` is local
to one entity. `originalData` is the current source row, not the initial snapshot. Deleted entities
return `originalRecordPresent=false` and `originalData=null`.
Without an audit table, deleted records cannot be returned because no persisted history remains.

## 6. Dynamic table discovery and Oracle access

Supported original tables are discovered from `USER_TABLES` on each catalog request. There is no
Java enum or configuration allowlist to maintain. Discovery uses the configured prefix and
excludes names ending in the configured audit suffix. Labels are derived by removing the prefix,
splitting on underscores, title-casing each segment, and joining with `-`. The audit name is
always derived internally as `<SOURCE_TABLE><AUDIT_SUFFIX>`.

The runtime Oracle account must be least-privileged for generic audit queries and must connect as
the schema owner because discovery intentionally uses `USER_TABLES`/`USER_TAB_COLUMNS`. A
cross-schema `SELECT` grant or `CURRENT_SCHEMA` change does not populate these views.
The production application does not create, modify, or seed database tables or rows. There is no
runtime sample-data loader, dummy-data SQL file, or hardcoded response row. All generic endpoint
values come from Oracle queries; JSON files under `docs` describe contracts only.

Before exposing a table, the DBA must confirm that its row-level trigger is enabled, valid,
tested for all three DML operations, and writes the required full snapshot atomically. Operations
must monitor invalid/disabled triggers and periodically reconcile source/audit coverage.

## 7. Validation and error behavior

| Condition | HTTP | JSON status | Application code |
|---|---:|---|---:|
| Success | 200/201 | `SUCCESS` | `2000` |
| Redirect category (reserved) | 3xx | `REDIRECTION` | `3000` |
| Invalid identifier | 400 | `INVALID_TABLE_NAME` | `4000` |
| Invalid page number | 400 | `INVALID_PAGE_NO` | `4001` |
| Invalid page size | 400 | `INVALID_PAGE_SIZE` | `4002` |
| Audit table passed directly | 400 | `AUDIT_TABLE_NOT_ACCEPTED` | `4003` |
| Invalid request body | 400 | `VALIDATION_FAILED` | `4004` |
| Malformed request or parameter type | 400 | `INVALID_REQUEST` | `4005` |
| Table not discovered | 404 | `TABLE_NOT_ALLOWED` | `4007` |
| Source/audit pair missing | 404 | `TABLE_PAIR_NOT_FOUND` | `4008` |
| Required audit column missing | 422 | `MISSING_REQUIRED_COLUMN` | `4009` |
| Holiday missing | 404 | `HOLIDAY_NOT_FOUND` | `4010` |
| Commodity debit report data missing | 404 | `COMMODITY_DEBIT_NOT_FOUND` | `4011` |
| Unexpected exception | 500 | `INTERNAL_ERROR` | `5000` |
| Oracle/JDBC failure | 500 | `DATABASE_ERROR` | `5001` |

Application code ranges are `2xxx` success, `3xxx` redirection, `4xxx` client errors, and `5xxx`
server errors. The numeric HTTP status is never duplicated in the body.

All validation failures are returned together in `details`. Internal failures return safe generic
messages. Logs identify the exception type without recording sensitive exception messages or stack
traces.

## 8. Input and database safety

- Reject table names outside the simple unquoted Oracle identifier grammar.
- Discover only tables visible to the connected schema through `USER_TABLES`; do not add an owner
  predicate or query cross-schema `ALL_TABLES`.
- Verify tables and required columns through Oracle metadata before constructing dynamic SQL.
- Bind row IDs and pagination values; never concatenate caller-controlled identifiers unchecked.

### 8.1 Logging safety

Use Lombok `@Slf4j` with parameterized messages at audit discovery, retrieval, and verified-metadata
boundaries. `INFO` logs may contain validated table identifiers, pagination values, and aggregate
counts. Error logs contain only stable application codes and exception types. Never log source or
audit row content, entity IDs, request bodies, SQL, bind values, credentials, JDBC URLs, raw
exception messages, or stack traces.

## 9. Pagination and performance

`audit-api.max-page-size` is externally configurable, defaults to `200`, and cannot exceed Oracle's
1,000-expression `IN` limit. The lower default protects JVM heap, connection occupancy, response
size, and latency because one ID may expand into many audit snapshots.

A warm audit request executes one ID-page query with an Oracle window count, one current-row query,
one audit-row query, and—only when supported—one narrow approval query. This removes the separate count round trip for normal pages. A page beyond
the available range performs one fallback count because an empty page has no window-count row.
Empty pages skip source, audit, and approval-row loading. Verified descriptors are cached, while a
missing approval-table result is rechecked on the next request. Required columns are fetched
together during resolution, and row data are never cached.
Revision summaries are accumulated in the same pass that groups history. Recommended audit
indexing begins with `(ID, REV)` and must be checked against actual Oracle execution plans and
load-test percentiles.

## 10. Configuration

| Property/environment variable | Purpose |
|---|---|
| `spring.datasource.url` / `ORACLE_URL` | Oracle JDBC URL |
| `spring.datasource.username` / `ORACLE_USERNAME` | Read-only database user |
| `spring.datasource.password` / `ORACLE_PASSWORD` | Database secret |
| `audit-api.source-table-prefix` / `AUDIT_SOURCE_TABLE_PREFIX` | Source-table discovery prefix |
| `audit-api.max-page-size` / `AUDIT_MAX_PAGE_SIZE` | Maximum accepted page size |
Approval tables follow fixed exact naming: `<SOURCE>_APPROVAL_REQUEST` or `<SOURCE>_APPROVAL`.
A source uses exactly one of these conventions, not both. The approval table needs the configured
ID and checker columns; maker is optional, and no approval `_AUD` companion is required for source-row
enrichment. Exceptional and abbreviated names are not mapped.

Secrets must come from environment or an enterprise secret manager and must never be committed.
Production JPA schema generation remains disabled.

### 10.1 Maintainable component design

- Controllers own HTTP mapping and common response wrapping only.
- `TableAuditService` defines the business contract; `TableAuditServiceImpl` owns validation,
  transaction boundaries, pagination, and orchestration.
- `AuditableTableCatalog` owns dynamic discovery, label formatting, and name resolution.
- `AuditHistoryAssembler` owns row indexing, audit grouping, revision sequencing, operation
  resolution, and change-summary construction.
- `TableAuditServiceImpl` applies optional maker/checker enrichment after source/audit assembly.
- `ApprovalTableResolver` and `ApprovalDao` isolate optional metadata resolution and bulk username
  lookup without changing the controller contract.
- DAO and JPA repository types exclusively own persistence access.
- `RevisionOperation` centrally maps Envers-compatible `REVTYPE` values to response operations.
- `GlobalExceptionHandler` centrally creates the common error envelope.
- `PdfGenerationService` owns reusable in-memory A4 PDF creation; `CommodityDebitPdfTemplate` owns
  only the commodity debit layout and receives an immutable database-backed DTO.
- Components use constructor injection and must not depend on controller or transport details.

These boundaries implement Single Responsibility and Dependency Inversion and must be preserved
when another source table is introduced.

## 11. Testing and dummy-data policy

- Runtime resources must contain no INSERT/UPDATE/DELETE seed script or application data loader.
- Unit tests may use in-memory fixtures, mocks, and H2 records under `src/test` only.
- Test fixtures must never connect to or mutate the configured production Oracle database.
- Controller tests verify the common envelope, nested `data`, status/code/message consistency,
  validation details, and absence of `path`.
- Full-context MockMvc tests verify table labels, malformed-request handling, and the complete
  Holiday CRUD and commodity PDF download flows against isolated H2 persistence.
- Service/DAO tests verify grouping, deleted IDs, revision ordering, full column preservation,
  pagination, SQL bindings, and metadata checks.
- A clean build targeting Java 21 must compile both `src/main` and `src/test`; `mvn clean verify`
  is the authoritative verification command. Maven may run on a compatible newer JDK while the
  compiler continues to use `--release 21`.
- Offline verification may use `mvn -o clean verify` only after all required dependencies are in
  the local Maven repository.
- IDE-only errors require Java 21 project/module SDK configuration and a Maven-model reload; build
  tool warnings must not be reported as application compilation errors.

## 12. Acceptance criteria

- Every success uses `timestamp`, `status=SUCCESS`, `code=2000`, a clear `message`, and
  endpoint-specific content only under `data`.
- Every failure uses the same core metadata, `data=null`, a safe error code/message, HTTP reason,
  and a non-null `details` array.
- GET audit requests require no body and validate path/query inputs.
- POST/PUT requests use validated request DTOs; controllers never expose JPA entities directly.
- Every source and audit column returned by Oracle is preserved without Java field changes.
- Oracle row-level triggers capture INSERT, UPDATE, and DELETE snapshots in the same transaction
  as the source change using `REVTYPE` values `0`, `1`, and `2`.
- Revision history is correctly grouped per ID and ordered independently of global revision reuse.
- Deleted IDs remain returned with `originalData=null`.
- Invalid identifiers cannot alter generated SQL.
- Repeated requests for a verified table reuse its successful source/audit descriptor metadata.
- The Maven verification build and all package-aligned tests pass.
- Commodity debit download returns a valid A4 PDF named `community.pdf`, with all dynamic values
  loaded from `PMC_COMMODITY_DEBIT` and missing IDs mapped to `COMMODITY_DEBIT_NOT_FOUND/4011`.
- JaCoCo reports and enforces 100% line and branch coverage for production logic; only the
  framework-delegating Spring Boot launcher is excluded.

## 13. Constraints and future extensions

- Version 1 supports a single-column `ID`; composite Envers keys require a key-strategy registry.
- Large histories may require future revision pagination or revision-range filters.
- Revision timestamp/user metadata can be joined when the exact revision-table schema is supplied.
