# Enterprise Audit History API

Java 21 and Spring Boot service for retrieving an Oracle source row together with its complete
Hibernate Envers audit history. It supports multiple discovered source/audit table pairs through one
generic endpoint and does not create, update, or seed Oracle audit data.

The authoritative requirements and acceptance criteria are in [docs/PRD.md](docs/PRD.md).

## Key capabilities

- Dynamic source-table discovery from Oracle `USER_TABLES`; no Java table enum or allowlist.
- Generic source and `<TABLE>_AUD` lookup for discovered Oracle tables.
- Full `SELECT *` source and audit snapshots without table-specific response classes.
- History grouped by entity ID and ordered by Envers revision.
- `REVTYPE` mapping: `0=INSERT`, `1=UPDATE`, `2=DELETE`, other values=`UNKNOWN`.
- Deleted entities retained through audit-only IDs.
- Zero-based pagination with a configurable safety limit.
- One consistent success/error response envelope with endpoint content under `data`.
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

For every exposed source table, the Oracle database provides a corresponding `<TABLE>_AUD` table
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
├── config              Validated audit configuration
├── constants           API paths and shared defaults
├── controller          Generic audit and typed Holiday controllers
├── dao                 Oracle JDBC access and Holiday JPA repository
├── dto                 Internal records plus request/response DTOs
├── entity              Typed JPA entities
├── enums               Response codes and revision operations
├── exception           Typed application errors and error DTOs
├── exceptionHandlers   Central REST exception mapping
├── resolver            Table metadata and REVTYPE resolution
├── service             Business contracts and response assembly
│   └── impl            Service implementations
└── validation          Pagination and Oracle identifier validation
```

## Design and maintainability

The backend keeps responsibilities small and uses constructor injection throughout:

| Component | Responsibility |
|---|---|
| `TableAuditController` | HTTP input/output only; delegates business work |
| `TableAuditService` | Stable business contract used by the controller |
| `TableAuditServiceImpl` | Validates input and coordinates catalog, metadata, pagination, DAO, and assembly |
| `AuditHistoryAssembler` | Groups source/audit rows by ID and builds immutable response DTOs |
| `TableAuditDao` | Executes parameterized source and audit queries |
| `AuditableTableCatalog` | Discovers source tables and resolves physical names and public labels |
| `TableDescriptorResolver` | Verifies the source/audit pair and required metadata |
| `RevisionOperationResolver` | Strategy abstraction for mapping `REVTYPE` to an operation |
| `GlobalExceptionHandler` | Creates and maps application/framework errors to the common response |

This applies the Single Responsibility and Dependency Inversion principles. The resolver is a
Strategy, consistent error construction stays in the global handler, and database access remains
behind DAO/Repository boundaries. Add behavior to the responsible component rather than adding
table-specific branches to the controller or service.

### Audit logging

Audit-flow components use Lombok `@Slf4j` and structured parameterized messages. `INFO` records
table discovery counts, validated source-table identifiers, pagination inputs, returned row counts,
total elements, and successful metadata verification. Failures log only the application code and
exception type. Logs never contain row or audit data, entity IDs, request bodies, SQL, bind values,
credentials, connection strings, exception messages, or stack traces.

## Response contract

All controller payloads are returned only under `data`. The surrounding metadata is implemented
once through `BaseApiResponse` and successful payloads use `ApiResponse<T>`.

| Field | Success | Error | Purpose |
|---|---:|---:|---|
| `timestamp` | Yes | Yes | ISO-8601 response creation time |
| `status` | `SUCCESS` | Specific failure status | Outcome matching the message |
| `code` | `2000` | `4xxx` or `5xxx` | Stable application response code |
| `message` | Yes | Yes | Safe result description |
| `data` | Payload/null | `null` | Endpoint-specific response object |
| `error` | No | Yes | Standard HTTP reason phrase |
| `details` | No | Yes | Validation failures or an empty array |

### Success

```json
{
  "timestamp": "<ISO-8601 timestamp>",
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": {}
}
```

POST creation responses use HTTP `201`. Other successful endpoints use HTTP `200`. The numeric
HTTP status is not repeated in the JSON body.

### Error

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

Validation errors place every rejected field in `details`. Other failures return an empty array.
The response never exposes the request path, SQL, credentials, stack traces, or Oracle messages.

Application codes are stable four-digit strings defined by `ApiOutcomeCode`. `status` identifies
the exact outcome that corresponds to `message`; the numeric protocol status remains in the HTTP
response line.

See [API Request and Response Examples](docs/API_RESPONSE_EXAMPLES.md) for every endpoint,
request body, success response, and supported error response.

## API endpoints

### Retrieve source rows with complete audit history

```http
GET /api/v1/{tableName}?pageNo=0&pageSize=10
```

This GET endpoint has no request body.

| Parameter | Location | Required | Description |
|---|---|---:|---|
| `tableName` | Path | Yes | Discovered source name or returned label; do not pass the audit table |
| `pageNo` | Query | No | Zero-based page number; default `0` |
| `pageSize` | Query | No | Page size; default `10`; maximum is configured |

Example request without database-row data:

```bash
curl 'http://localhost:8080/api/v1/Loco-Singapore?pageNo=0&pageSize=10'
```

The `data` object is `SearchResponse`:

| Field | Meaning |
|---|---|
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
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": {
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
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": {
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

This endpoint has no request body and no pagination. It discovers source tables from Oracle
`USER_TABLES` whose names start with the configured prefix (default `PMC_`) and do not end with
the audit suffix (default `_AUD`). It returns only alphabetically sorted formatted labels; audit
names are not exposed.
The same label can be passed directly to the detail endpoint, for example
`GET /api/v1/Loco-Singapore?pageNo=0&pageSize=10`.

HTTP `200` response:

```json
{
  "timestamp": "2026-09-14T04:30:00Z",
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": {
    "tableLabels": [
      "Account-Statement",
      "Loco-Singapore",
      "Position-Balance"
    ]
  }
}
```

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

`GET /api/v1/holidays?pageNo=0&pageSize=10` returns HTTP `200`:

```json
{
  "timestamp": "2026-09-14T04:32:00Z",
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": {
    "pageNo": 0,
    "pageSize": 10,
    "numberOfElements": 1,
    "totalElements": 1,
    "totalPages": 1,
    "hasPrevious": false,
    "hasNext": false,
    "rows": [
      {
        "id": 101,
        "holidayDate": "2026-12-25",
        "calendarCode": "SG",
        "calendarName": "Singapore Calendar",
        "version": 1,
        "createdBy": "operations-user",
        "createdOn": "2026-09-14T12:30:00",
        "updatedBy": "operations-user",
        "updatedOn": "2026-09-14T12:30:00"
      }
    ]
  }
}
```

`POST /api/v1/holidays` returns HTTP `201`; `PUT /api/v1/holidays/{id}` returns HTTP `200`.
Both return this `HolidayResponse` shape under `data`:

```json
{
  "timestamp": "2026-09-14T04:33:00Z",
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": {
    "id": 101,
    "holidayDate": "2026-12-25",
    "calendarCode": "SG",
    "calendarName": "Singapore Calendar",
    "version": 1,
    "createdBy": "operations-user",
    "createdOn": "2026-09-14T12:30:00",
    "updatedBy": "operations-user",
    "updatedOn": "2026-09-14T12:33:00"
  }
}
```

`DELETE /api/v1/holidays/{id}` returns HTTP `200`:

```json
{
  "timestamp": "2026-09-14T04:35:00Z",
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": null
}
```

Complete request bodies and every error response are maintained in
[API_RESPONSE_EXAMPLES.md](docs/API_RESPONSE_EXAMPLES.md).

## Exposing another auditable table

1. Create the source table using the configured prefix, default `PMC_`.
2. Provide its matching audit table using the configured suffix, default `_AUD`.
3. Confirm both tables share the configured `ID` column.
4. Confirm the audit table contains the configured `REV` and `REVTYPE` columns.
5. Run the API as a least-privileged account in the schema that owns the source/audit tables;
   `USER_TABLES` intentionally does not discover objects owned by another schema.
6. Validate the real Oracle execution plan and indexes.

No Java registry, configuration allowlist, or table-specific generic-audit class is needed. The
next request discovers the table dynamically.

## Configuration

`application.yml` supports environment overrides:

```bash
export ORACLE_URL='jdbc:oracle:thin:@//host:1521/service'
export ORACLE_USERNAME='audit_reader'
export ORACLE_PASSWORD='<secret-from-vault>'
export AUDIT_SOURCE_TABLE_PREFIX='PMC_'
export AUDIT_MAX_PAGE_SIZE=200
```

Important defaults:

| Setting | Default | Purpose |
|---|---|---|
| `audit-api.source-table-prefix` | `PMC_` | Prefix used to discover source tables |
| `audit-api.audit-suffix` | `_AUD` | Audit table naming convention |
| `audit-api.id-column` | `ID` | Shared entity identifier |
| `audit-api.audit-order-column` | `REV` | History ordering column |
| `audit-api.revision-type-column` | `REVTYPE` | Envers operation code |
| `audit-api.max-page-size` | `200` | Maximum IDs accepted per request |

The maximum page size cannot exceed Oracle's 1,000-expression `IN` limit. The lower default of
200 also limits heap usage, connection occupancy, JSON size, and latency when each ID has many
revisions.

`USER_TABLES` scopes discovery to tables owned by the connected Oracle user. Changing
`CURRENT_SCHEMA` or granting cross-schema `SELECT` does not make those tables appear in
`USER_TABLES`; use the owning schema account with only the privileges required by this API.

## Application response codes

| Condition | HTTP | JSON `status` | JSON `code` |
|---|---:|---|---:|
| Success | 200/201 | `SUCCESS` | `2000` |
| Redirect category (reserved) | 3xx | `REDIRECTION` | `3000` |
| Invalid table identifier | 400 | `INVALID_TABLE_NAME` | `4000` |
| Invalid page number | 400 | `INVALID_PAGE_NO` | `4001` |
| Invalid page size | 400 | `INVALID_PAGE_SIZE` | `4002` |
| Audit table passed directly | 400 | `AUDIT_TABLE_NOT_ACCEPTED` | `4003` |
| Invalid request body | 400 | `VALIDATION_FAILED` | `4004` |
| Invalid parameter type or malformed request | 400 | `INVALID_REQUEST` | `4005` |
| Table not discovered | 404 | `TABLE_NOT_ALLOWED` | `4007` |
| Source/audit pair missing | 404 | `TABLE_PAIR_NOT_FOUND` | `4008` |
| Required audit column missing | 422 | `MISSING_REQUIRED_COLUMN` | `4009` |
| Holiday missing | 404 | `HOLIDAY_NOT_FOUND` | `4010` |
| Unexpected server failure | 500 | `INTERNAL_ERROR` | `5000` |
| Oracle/JDBC failure | 500 | `DATABASE_ERROR` | `5001` |

The ranges are reserved by outcome category: `2000–2999` success, `3000–3999` redirection,
`4000–4999` client errors, and `5000–5999` server errors.

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

The production baseline is Java 21. Build with JDK 21 or a newer JDK capable of compiling with
`--release 21`, plus Maven 3.9 or the project-compatible Maven version. Confirm the active
toolchain before building:

```bash
java -version
mvn -version
```

The Maven output must show compilation with `release 21`. From the project directory, compile
production and test sources and run every quality gate with:

```bash
mvn clean verify
```

When all dependencies are already present in the local Maven cache, the reproducible offline form
is:

```bash
mvn -o clean verify
```

Run the packaged service:

```bash
java -jar target/enterprise-audit-history-api-0.0.1-SNAPSHOT.jar
```

Tests are organized by production package. Controller and end-to-end tests verify JSON contracts,
service tests verify grouping and history semantics, and DAO tests verify SQL behavior. JaCoCo
generates the HTML report at `target/site/jacoco/index.html` and
fails `mvn verify` unless both line and branch coverage remain at 100%. The Spring Boot launcher is
excluded because it contains only the framework-delegating `main` method.

If Maven succeeds but an IDE still reports compilation errors, set the Project SDK and language
level to Java 21, reload the Maven model from `pom.xml`, and rebuild the project. Maven's clean
build is the authoritative compilation result. Jansi restricted-native-access and Mockito
self-attachment messages on newer JDK runtimes are dependency compatibility warnings, not Java
source compilation failures.

## Database-data policy

- Production resources contain no sample-data loader or dummy-data DML script.
- The application uses `ddl-auto: none` and does not create or update Oracle schemas.
- Test fixtures live only under `src/test` and use mocks or an isolated H2 database.
- Test execution must never point to or mutate a production Oracle database.
- The remaining Oracle SQL resource contains index guidance only and must be reviewed by a DBA
  before use.
