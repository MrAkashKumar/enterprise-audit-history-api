# Enterprise Audit History API

Java 21 and Spring Boot service for retrieving an Oracle source row together with its complete
Hibernate Envers audit history. It supports multiple approved source/audit table pairs through one
generic endpoint and does not create, update, or seed Oracle audit data.

The authoritative requirements and acceptance criteria are in [docs/PRD.md](docs/PRD.md).

## Key capabilities

- Generic source and `<TABLE>_AUD` lookup for centrally approved Oracle tables.
- Full `SELECT *` source and audit snapshots without table-specific response classes.
- History grouped by entity ID and ordered by Envers revision.
- `REVTYPE` mapping: `0=INSERT`, `1=UPDATE`, `2=DELETE`, other values=`UNKNOWN`.
- Deleted entities retained through audit-only IDs.
- Zero-based pagination with a configurable safety limit.
- One consistent success/error response envelope with endpoint content under `data`.
- Error-only trace IDs generated using `SecureRandom` and added to MDC logs.
- Optional API-key authentication.
- JPA for the typed Holiday CRUD example and `JdbcTemplate` for generic dynamic audit reads.

## Technology

- Java 21
- Spring Boot 3.5
- Spring Web and Jakarta Validation
- Spring Data JPA
- Spring JDBC / `NamedParameterJdbcTemplate`
- Oracle JDBC driver
- Maven
- JUnit 5, Mockito, MockMvc, AssertJ, and H2 for isolated tests

## Project structure

```text
src/main/java/com/akash/auditapi
├── config       API paths and validated configuration
├── controller   Generic audit REST controller
├── dao          Oracle metadata and dynamic audit queries
├── exception    Stable errors and global exception handling
├── holiday      Typed JPA request/entity/response example
├── model        API envelopes and audit response DTOs
├── resolver     Table metadata and REVTYPE resolution
├── security     Optional API-key filter
├── service      Audit-history orchestration
├── trace        Secure trace-ID generation and MDC lifecycle
└── validation   Pagination and Oracle identifier validation
```

## Response contract

All controller payloads are returned only under `data`. The surrounding metadata is implemented
once through `BaseApiResponse<C>` and successful payloads use `ApiResponse<T>`.

### Success

```json
{
  "timestamp": "<ISO-8601 timestamp>",
  "status": 200,
  "code": "SUCCESS",
  "message": "Request completed successfully",
  "data": {}
}
```

POST creation responses use HTTP/body status `201`. Other successful endpoints use `200`.
Successful responses do not expose a trace ID.

### Error

```json
{
  "timestamp": "<ISO-8601 timestamp>",
  "status": 500,
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "data": null,
  "traceId": "<error correlation ID>",
  "error": "Internal Server Error",
  "details": []
}
```

Validation errors place every rejected field in `details`. Other failures return an empty array.
The response never exposes the request path, SQL, credentials, stack traces, or Oracle messages.

Client-facing codes are readable and stable. Internally, `SUCCESS` maps to support code `1000` and
the unknown-exception fallback maps to `0000` in backend logs. API consumers should use the
client-facing `code` field.

## API endpoints

### Retrieve source rows with complete audit history

```http
GET /api/v1/{tableName}?pageNo=0&pageSize=10
```

This GET endpoint has no request body.

| Parameter | Location | Required | Description |
|---|---|---:|---|
| `tableName` | Path | Yes | Approved original table name; do not pass the audit table |
| `pageNo` | Query | No | Zero-based page number; default `0` |
| `pageSize` | Query | No | Page size; default `10`; maximum is configured |

Example request without database-row data:

```bash
curl --header 'X-API-Key: <configured-key>' \
  'http://localhost:8080/api/v1/PMC_LOCO_SINGAPORE?pageNo=0&pageSize=10'
```

The `data` object is `SearchResponse`:

| Field | Meaning |
|---|---|
| `sourceTable` | Resolved source table |
| `auditTable` | Derived and verified audit table |
| `pageNo`, `pageSize` | Applied pagination |
| `numberOfElements` | Distinct IDs returned on the current page |
| `totalElements`, `totalPages` | Overall distinct-ID totals |
| `hasPrevious`, `hasNext` | Navigation flags |
| `rows` | Current rows and their complete history grouped by ID |

Each `rows` item contains:

- `id`: entity identifier.
- `originalRecordPresent`: whether the row still exists in the source table.
- `originalData`: every current source-table column, or `null` after deletion.
- `changeSummary`: revision counts and first/latest revision.
- `auditHistory`: ordered history with sequence, revision, operation, and every audit column.

Empty result shape:

```json
{
  "timestamp": "<ISO-8601 timestamp>",
  "status": 200,
  "code": "SUCCESS",
  "message": "Request completed successfully",
  "data": {
    "sourceTable": "PMC_LOCO_SINGAPORE",
    "auditTable": "PMC_LOCO_SINGAPORE_AUD",
    "pageNo": 0,
    "pageSize": 10,
    "numberOfElements": 0,
    "totalElements": 0,
    "totalPages": 0,
    "hasPrevious": false,
    "hasNext": false,
    "rows": []
  }
}
```

### Retrieve supported table labels

```http
GET /api/v1/allTable
```

This endpoint has no request body and no pagination. It returns `data.tableLabels` from the
`AuditableTable` enum. Only original table labels are returned; audit names are not returned.

### Holiday CRUD example

```http
GET    /api/v1/holidays?pageNo=0&pageSize=10
POST   /api/v1/holidays
PUT    /api/v1/holidays/{id}
DELETE /api/v1/holidays/{id}
```

GET and DELETE have no request body. POST and PUT accept `HolidayRequest`:

| Field | Type | Validation |
|---|---|---|
| `id` | integer | Required and positive |
| `holidayDate` | ISO local date | Required |
| `calendarCode` | string | Required, non-blank, maximum 50 characters |
| `calendarName` | string | Required, non-blank, maximum 200 characters |
| `username` | string | Required, non-blank, maximum 128 characters |

The controller maps JPA entities to `HolidayResponse`; persistence entities are never returned
directly. Paginated reads return `PageResponse<HolidayResponse>` under `data`. DELETE returns a
success envelope with `data: null`.

## Registering another auditable table

1. Add the original Oracle table and user-facing label to `AuditableTable`.
2. Add the original table name to `AUDIT_ALLOWED_TABLES`.
3. Confirm the source and audit tables share the configured `ID` column.
4. Confirm the audit table contains the configured `REV` and `REVTYPE` columns.
5. Confirm the audit table uses the configured suffix, default `_AUD`.
6. Grant the runtime read-only account `SELECT` access and metadata visibility.
7. Validate the real Oracle execution plan and indexes.

No table-specific generic-audit controller, service, DAO, entity, or response class is needed.

## Configuration

`application.yml` supports environment overrides:

```bash
export ORACLE_URL='jdbc:oracle:thin:@//host:1521/service'
export ORACLE_USERNAME='audit_reader'
export ORACLE_PASSWORD='<secret-from-vault>'
export AUDIT_ALLOWED_TABLES='PMC_HOLIDAY_CALENDAR,PMC_LOCO_SINGAPORE,PMC_POSITION_BALANCE'
export AUDIT_MAX_PAGE_SIZE=200
export AUDIT_API_KEY_ENABLED=true
export AUDIT_API_KEY='<secret-from-vault>'
```

Important defaults:

| Setting | Default | Purpose |
|---|---|---|
| `audit-api.audit-suffix` | `_AUD` | Audit table naming convention |
| `audit-api.id-column` | `ID` | Shared entity identifier |
| `audit-api.audit-order-column` | `REV` | History ordering column |
| `audit-api.revision-type-column` | `REVTYPE` | Envers operation code |
| `audit-api.max-page-size` | `200` | Maximum IDs accepted per request |
| `audit-api.security.enabled` | `false` | Enables API-key validation |
| `audit-api.security.header-name` | `X-API-Key` | API-key request header |

The maximum page size cannot exceed Oracle's 1,000-expression `IN` limit. The lower default of
200 also limits heap usage, connection occupancy, JSON size, and latency when each ID has many
revisions.

## Trace IDs

- Callers may send `X-Trace-Id` using 1–64 safe characters.
- Missing or invalid values are replaced by a 128-bit `SecureRandom` hexadecimal ID.
- The value is added to MDC so every request log can be correlated.
- MDC is cleared in a `finally` block to prevent thread-pool leakage.
- `X-Trace-Id` and JSON `traceId` are returned only when an error occurs.

## Error codes

| Condition | HTTP | Code |
|---|---:|---|
| Invalid table identifier | 400 | `INVALID_TABLE_NAME` |
| Invalid pagination | 400 | `INVALID_PAGE_NUMBER` / `INVALID_PAGE_SIZE` |
| Invalid request body | 400 | `VALIDATION_FAILED` |
| Invalid parameter type or malformed request | 400 | `INVALID_REQUEST` |
| Audit table passed directly | 400 | `AUDIT_TABLE_NOT_ACCEPTED` |
| Missing or invalid API key | 401 | `UNAUTHORIZED` |
| Table not approved | 404 | `TABLE_NOT_ALLOWED` |
| Source/audit pair missing | 404 | `TABLE_PAIR_NOT_FOUND` |
| Holiday missing | 404 | `HOLIDAY_NOT_FOUND` |
| Required audit column missing | 422 | `MISSING_REQUIRED_COLUMN` |
| Oracle/JDBC failure | 500 | `DATABASE_ERROR` |
| Unknown exception | 500 | `INTERNAL_ERROR` |

## Query and performance behavior

A warm generic audit request performs:

1. One distinct-ID count query.
2. One paged-ID query.
3. One current-source-row query.
4. One complete audit-history query.

Empty pages skip the last two queries. Successfully verified table descriptors are cached; source
and audit records are never cached. Audit indexes should begin with `(ID, REV)`, subject to DBA
review of existing indexes and real execution plans.

## Build and test

From the project directory:

```bash
mvn clean verify
```

Run the packaged service:

```bash
java -jar target/enterprise-audit-history-api-0.0.1-SNAPSHOT.jar
```

Tests are organized by production package. Controller tests verify JSON contracts, service tests
verify grouping and history semantics, DAO tests verify SQL behavior, and security/trace tests
verify error correlation.

## Database-data policy

- Production resources contain no sample-data loader or dummy-data DML script.
- The application uses `ddl-auto: none` and does not create or update Oracle schemas.
- Test fixtures live only under `src/test` and use mocks or an isolated H2 database.
- Test execution must never point to or mutate a production Oracle database.
- The remaining Oracle SQL resource contains index guidance only and must be reviewed by a DBA
  before use.
