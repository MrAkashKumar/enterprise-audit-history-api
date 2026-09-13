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

## Oracle enterprise audit architecture

This project uses the recommended **row-level trigger + audit table** approach. Oracle owns audit
capture; the Spring Boot API only reads the resulting source and audit tables.

```text
Application DML
      │
      ▼
Oracle source table
      │  AFTER INSERT OR UPDATE OR DELETE FOR EACH ROW
      ▼
Oracle row-level audit trigger
      │
      ▼
<SOURCE_TABLE>_AUD
      │
      ▼
Enterprise Audit History API (read only)
```

For every approved source table, the Oracle database provides a corresponding `<TABLE>_AUD` table
and row-level trigger. The trigger writes a full snapshot with the entity `ID`, revision `REV`, and
operation `REVTYPE`:

| DML operation | Trigger image | `REVTYPE` |
|---|---|---:|
| `INSERT` | `:NEW` values | `0` |
| `UPDATE` | Updated `:NEW` values | `1` |
| `DELETE` | Final `:OLD` values | `2` |

The audit insert should participate in the same Oracle transaction as the source change so both
commit or roll back together. Audit tables should be append-only for application accounts; normal
application users must not receive update/delete access to history. Revision allocation, audit
trigger deployment, retention, partitioning, and optional actor/timestamp columns are owned by the
database/DBA platform. The Java service never creates triggers and never writes audit rows.

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

Current and deleted records use the same row shape:

| Record state | `originalRecordPresent` | `originalData` | `auditHistory` |
|---|---:|---|---|
| Present in source table | `true` | Complete current source row | Complete ordered history |
| Deleted from source table | `false` | `null` | Complete history, including the `DELETE` snapshot |

Representative populated response (the dynamic column names come from the selected Oracle tables):

```json
{
  "timestamp": "2026-09-14T00:15:30.412Z",
  "status": 200,
  "code": "SUCCESS",
  "message": "Request completed successfully",
  "data": {
    "sourceTable": "PMC_LOCO_SINGAPORE",
    "auditTable": "PMC_LOCO_SINGAPORE_AUD",
    "pageNo": 0,
    "pageSize": 10,
    "numberOfElements": 1,
    "totalElements": 1,
    "totalPages": 1,
    "hasPrevious": false,
    "hasNext": false,
    "rows": [
      {
        "id": 1001,
        "originalRecordPresent": true,
        "originalData": {
          "ID": 1001,
          "VERSION": 2,
          "STATUS": "ACTIVE"
        },
        "changeSummary": {
          "totalRevisions": 2,
          "insertCount": 1,
          "updateCount": 1,
          "deleteCount": 0,
          "unknownCount": 0,
          "firstRevision": 9063,
          "latestRevision": 9071
        },
        "auditHistory": [
          {
            "sequenceNumber": 1,
            "revision": 9063,
            "revisionTypeCode": 0,
            "operation": "INSERT",
            "ID": 1001,
            "VERSION": 1,
            "STATUS": "ACTIVE",
            "REV": 9063,
            "REVTYPE": 0
          },
          {
            "sequenceNumber": 2,
            "revision": 9071,
            "revisionTypeCode": 1,
            "operation": "UPDATE",
            "ID": 1001,
            "VERSION": 2,
            "STATUS": "ACTIVE",
            "REV": 9071,
            "REVTYPE": 1
          }
        ]
      }
    ]
  }
}
```

`ID`, `VERSION`, and `STATUS` above are illustrative. At runtime, `originalData` contains every
column returned by the source query and each `auditHistory` item contains every column returned by
the audit query, including database `null` values. The API adds only the four derived history
fields: `sequenceNumber`, `revision`, `revisionTypeCode`, and `operation`.

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
The same label can be URL-encoded and passed to the detail endpoint, for example
`GET /api/v1/Loco%20Singapore?pageNo=0&pageSize=10`.

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

1. One paged-ID query that also returns the unpaged total using an Oracle window count.
2. One current-source-row query.
3. One complete audit-history query.

This removes one database round trip from normal requests. A page beyond the available range uses
one fallback count query because Oracle returns no window-count value for an empty result page.
Empty pages skip the source and history queries. Successfully verified table descriptors are
cached, and required columns are loaded in one metadata query during first resolution; source and
audit records are never cached. Revision summaries are accumulated while history is grouped rather
than by rescanning each entity history. Audit indexes should begin with `(ID, REV)`, subject to DBA
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
