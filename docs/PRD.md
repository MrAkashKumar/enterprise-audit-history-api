# Product Requirements Document: Enterprise Audit History API

## 1. Purpose

Provide a reusable Java 21 and Spring Boot REST API that returns the current row from an approved
Oracle source table together with its complete Hibernate Envers history from `<TABLE>_AUD`. The
generic audit endpoint must support multiple approved tables without table-specific controllers,
entities, repositories, or response models.

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
| `traceId` | string | No | Yes | Error correlation ID also present in logs/header |
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
  "traceId": "<error correlation ID>",
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

## 4. Endpoints and request contracts

### 4.1 Source row with audit history

```http
GET /api/v1/{tableName}?pageNo=0&pageSize=10
```

This GET endpoint has no request body.

| Input | Location | Required | Rules |
|---|---|---:|---|
| `tableName` | Path | Yes | Original table only; simple Oracle identifier; centrally allowlisted |
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

Each item in `rows` contains `id`, `originalRecordPresent`, `originalData`, `changeSummary`, and
`auditHistory`. `originalData` includes every source column returned by Oracle. Each history item
contains the derived sequence/revision/operation fields and every audit-table column.

The row contract is state-independent:

| Entity state | Required API representation |
|---|---|
| Current source row exists | `originalRecordPresent=true`; `originalData` is the complete current row |
| Source row was deleted | `originalRecordPresent=false`; `originalData=null`; history remains complete |
| No audit revisions exist | Zero-valued `changeSummary`; empty `auditHistory` |
| Unknown `REVTYPE` exists | Preserve the row and return `operation=UNKNOWN`; increment `unknownCount` |

The service must preserve database `null` values and source column order in map-backed snapshots.
It must not replace full row data with a reduced, table-specific projection. The success response
must not contain `sourceTable`, `auditTable`, `traceId`, `path`, or `processingTimeMs`. Source and
audit table names remain internal query metadata.

### 4.2 Supported table labels

```http
GET /api/v1/allTable
```

No request body or pagination is accepted. `data.tableLabels` contains the user-facing labels from
`AuditableTable`; audit table names are not exposed by this endpoint.

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

## 5. Audit behavior and data semantics

### 5.1 Recommended Oracle architecture

The production auditing model is **row-level trigger + audit table**. Oracle is the system of
record for audit capture. Each approved source table has:

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

1. Resolve the audit table using `<SOURCE_TABLE>_AUD`.
2. Require a shared single-column `ID` and audit columns `REV` and `REVTYPE`.
3. Page distinct non-null IDs across the union of source and audit tables.
4. Include audit-only IDs so deleted entities remain discoverable.
5. Load complete source rows and complete audit snapshots using `SELECT *`.
6. Group source and history rows using a normalized representation of the ID.
7. Sort audit history by `REV` ascending and assign a one-based `sequenceNumber` per entity.
8. Map `REVTYPE`: `0=INSERT`, `1=UPDATE`, `2=DELETE`; other values become `UNKNOWN`.
9. Calculate total, insert, update, delete, and unknown counts plus first/latest revisions.

`REV` is a global transaction revision and can occur for multiple IDs. `sequenceNumber` is local
to one entity. `originalData` is the current source row, not the initial snapshot. Deleted entities
return `originalRecordPresent=false` and `originalData=null`.

## 6. Table registration and Oracle access

Supported original tables are registered centrally in `AuditableTable` and in the configured
allowlist. Current configured examples are `HOLIDAY_CALENDAR`, `LOCO_SINGAPORE`, and
`POSITION_BALANCE`; their audit names are derived rather than stored in the enum.

The runtime Oracle account must be least-privileged and read-only for generic audit queries.
The production application does not create, modify, or seed database tables or rows. There is no
runtime sample-data loader and no dummy-data SQL file.

Before registering a table, the DBA must confirm that its row-level trigger is enabled, valid,
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
| API key missing/invalid | 401 | `UNAUTHORIZED` | `4006` |
| Table not allowlisted | 404 | `TABLE_NOT_ALLOWED` | `4007` |
| Source/audit pair missing | 404 | `TABLE_PAIR_NOT_FOUND` | `4008` |
| Required audit column missing | 422 | `MISSING_REQUIRED_COLUMN` | `4009` |
| Holiday missing | 404 | `HOLIDAY_NOT_FOUND` | `4010` |
| Unexpected exception | 500 | `INTERNAL_ERROR` | `5000` |
| Oracle/JDBC failure | 500 | `DATABASE_ERROR` | `5001` |

Application code ranges are `2xxx` success, `3xxx` redirection, `4xxx` client errors, and `5xxx`
server errors. The numeric HTTP status is never duplicated in the body.

All validation failures are returned together in `details`. Internal failures return safe generic
messages; full exceptions are logged server-side with the trace ID.

## 8. Security and tracing

- Reject table names outside the simple unquoted Oracle identifier grammar.
- Verify tables and required columns through Oracle metadata before constructing dynamic SQL.
- Bind row IDs and pagination values; never concatenate caller-controlled identifiers unchecked.
- Optionally require a configurable `X-API-Key` for `/api/v1/*`.
- Compare API keys using constant-time byte comparison.
- Accept only safe caller trace IDs; otherwise generate 128 bits using `SecureRandom` and lowercase
  hexadecimal encoding.
- Put the trace ID in MDC for request logs and clear it in a `finally` block.
- Return `X-Trace-Id` and JSON `traceId` only for errors.

## 9. Pagination and performance

`audit-api.max-page-size` is externally configurable, defaults to `200`, and cannot exceed Oracle's
1,000-expression `IN` limit. The lower default protects JVM heap, connection occupancy, response
size, and latency because one ID may expand into many audit snapshots.

A warm audit request executes one ID-page query with an Oracle window count, one current-row query,
and one audit-row query. This removes the separate count round trip for normal pages. A page beyond
the available range performs one fallback count because an empty page has no window-count row.
Empty pages skip source and audit-row loading. Verified table descriptors are cached, required
columns are fetched together during first resolution, and source/audit data are never cached.
Revision summaries are accumulated in the same pass that groups history. Recommended audit
indexing begins with `(ID, REV)` and must be checked against actual Oracle execution plans and
load-test percentiles.

## 10. Configuration

| Property/environment variable | Purpose |
|---|---|
| `spring.datasource.url` / `ORACLE_URL` | Oracle JDBC URL |
| `spring.datasource.username` / `ORACLE_USERNAME` | Read-only database user |
| `spring.datasource.password` / `ORACLE_PASSWORD` | Database secret |
| `audit-api.allowed-tables` / `AUDIT_ALLOWED_TABLES` | Source-table allowlist |
| `audit-api.max-page-size` / `AUDIT_MAX_PAGE_SIZE` | Maximum accepted page size |
| API-key settings / `AUDIT_API_KEY_ENABLED`, `AUDIT_API_KEY` | Optional endpoint protection |

Secrets must come from environment or an enterprise secret manager and must never be committed.
Production JPA schema generation remains disabled.

### 10.1 Maintainable component design

- Controllers own HTTP mapping and common response wrapping only.
- `TableAuditService` owns validation, transaction boundaries, pagination, and orchestration.
- `AuditHistoryAssembler` owns row indexing, audit grouping, revision sequencing, operation
  resolution, and change-summary construction.
- DAO and JPA repository types exclusively own persistence access.
- `RevisionOperationResolver` provides the operation-mapping Strategy and is injected by
  interface.
- `ApiErrorFactory` is the single Factory for error envelopes used by exception and security
  handling.
- Components use constructor injection and must not depend on controller or transport details.

These boundaries implement Single Responsibility and Dependency Inversion and must be preserved
when another source table or revision scheme is introduced.

## 11. Testing and dummy-data policy

- Runtime resources must contain no INSERT/UPDATE/DELETE seed script or application data loader.
- Unit tests may use in-memory fixtures, mocks, and H2 records under `src/test` only.
- Test fixtures must never connect to or mutate the configured production Oracle database.
- Controller tests verify the common envelope, nested `data`, status/code/message consistency,
  error-only tracing, validation details, and absence of `path`.
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
- Every failure uses the same core metadata, `data=null`, a safe error code/message, `traceId`,
  HTTP reason, and a non-null `details` array.
- GET audit requests require no body and validate path/query inputs.
- POST/PUT requests use validated request DTOs; controllers never expose JPA entities directly.
- Every source and audit column returned by Oracle is preserved without Java field changes.
- Oracle row-level triggers capture INSERT, UPDATE, and DELETE snapshots in the same transaction
  as the source change using `REVTYPE` values `0`, `1`, and `2`.
- Revision history is correctly grouped per ID and ordered independently of global revision reuse.
- Deleted IDs remain returned with `originalData=null`.
- Invalid identifiers cannot alter generated SQL.
- Repeated requests for an approved table do not repeat successful metadata discovery.
- The Maven verification build and all package-aligned tests pass.
- JaCoCo reports and enforces 100% line and branch coverage for production logic; only the
  framework-delegating Spring Boot launcher is excluded.

## 13. Constraints and future extensions

- Version 1 supports a single-column `ID`; composite Envers keys require a key-strategy registry.
- Large histories may require future revision pagination or revision-range filters.
- Revision timestamp/user metadata can be joined when the exact revision-table schema is supplied.
- A gateway or OAuth2 resource server should replace shared API keys where enterprise identity is
  available.
