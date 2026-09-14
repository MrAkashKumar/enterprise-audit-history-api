# Portable Build Prompt: Enterprise Audit History API

Copy this entire file into a coding assistant to create or update the backend. Treat it as the
authoritative implementation prompt.

---

Act as a senior Java software engineer and solution designer. Build a production-quality backend
named **Enterprise Audit History API**. Inspect an existing project before changing it, preserve
correct behavior, remove obsolete code, and implement only the requirements below. For a new
project, create `enterprise-audit-history-api` using Maven.

## 1. Goal and scope

Create a reusable, read-focused Spring Boot API that returns complete current rows from approved
Oracle source tables together with complete history from their existing audit tables. One generic
endpoint must support all centrally registered tables without a separate entity, repository,
service, controller, or fixed-column DTO for each table.

Example pair:

- Source: `PMC_LOCO_SINGAPORE`
- Audit: `PMC_LOCO_SINGAPORE_AUD`

Oracle source tables, audit tables, row triggers, revisions, and production data already exist.
The application must not create, alter, seed, or write to audit tables. Do not add dummy runtime
data.

## 2. Technology and structure

- Java 21
- Spring Boot 3.5.x
- Maven
- Base package `com.akash.auditapi`
- Maven coordinates `com.akash:enterprise-audit-history-api`
- Spring Web and Jakarta Validation
- Spring JDBC with `JdbcTemplate` and `NamedParameterJdbcTemplate`
- Spring Data JPA for typed, compile-time-known CRUD such as Holiday
- Oracle JDBC driver at runtime
- JUnit 5, Mockito, MockMvc, AssertJ, and isolated H2 tests

JPA and JDBC must coexist. Use JPA for known entities and JDBC for generic audit reads because
table names and columns vary at runtime. Never try to bind a table name as a SQL parameter.

Use clear packages for configuration, controller, service, DAO, model, resolver, validation,
exception, trace, security, and the typed Holiday example. Keep tests in matching packages.

Apply maintainable boundaries and SOLID principles:

- Keep controllers limited to HTTP mapping and response wrapping.
- Keep `TableAuditService` limited to validation, transaction/pagination orchestration, metadata
  resolution, and DAO coordination.
- Put source indexing, audit grouping, revision sequencing, and summary construction in a focused
  `AuditHistoryAssembler`.
- Hide database access behind DAO/JPA repository boundaries.
- Inject a `RevisionOperationResolver` Strategy instead of branching on revision schemes in the
  service.
- Use one `ApiErrorFactory` for exception-handler and security-filter error envelopes.
- Use constructor injection and keep dependencies directed toward interfaces or focused
  collaborators.

## 3. Oracle audit model

Document and support the enterprise **row-level trigger + audit table** approach:

1. Application DML changes a source row.
2. An Oracle `AFTER INSERT OR UPDATE OR DELETE FOR EACH ROW` trigger writes a full audit snapshot.
3. INSERT and UPDATE use `:NEW`; DELETE uses `:OLD`.
4. The audit write participates in the source transaction.
5. The Java API reads but never maintains audit history.

Required conventional columns:

- `ID`: shared source/audit entity identifier
- `REV`: orderable revision number
- `REVTYPE`: operation code

Map operations exactly:

- `0` -> `INSERT`
- `1` -> `UPDATE`
- `2` -> `DELETE`
- Other or invalid values -> `UNKNOWN`

Do not generate revisions or Oracle triggers in Java. The DBA platform owns triggers, revision
allocation, retention, partitioning, reconciliation, and audit-table permissions.

## 4. Generic audit endpoint

Implement in `TableAuditController`:

```http
GET /api/v1/{tableName}?pageNo=0&pageSize=10
```

The GET request has no body. Requirements:

- Accept only an approved original table name or registered public label.
- Reject an audit-table name passed directly.
- Use zero-based `pageNo`, default `0`.
- Use `pageSize`, default `10`.
- Validate `pageNo >= 0` and `1 <= pageSize <= configured maximum`.
- Derive the audit table with the configurable suffix, default `_AUD`.
- Page by distinct IDs across the union of source and audit tables.
- Include audit-only IDs so deleted records remain discoverable.
- Fetch complete source and audit rows using `SELECT *`.
- Group current data and history by the shared ID.
- Order each entity history by `REV` ascending.
- Assign `sequenceNumber` from 1 independently for each entity.
- Derive operation fields without removing physical `REV` or `REVTYPE` columns.
- Preserve database `null` values and source column order.
- Do not add relationship expansion, dependent joins, recursive graphs, `includeRelations`, or a
  `relationships` response field.

Controller shape:

```java
@GetMapping("/{tableName}")
public ResponseEntity<ApiResponse<SearchResponse>> sourceWithAuditHistory(
        @PathVariable String tableName,
        @RequestParam(defaultValue = "0") int pageNo,
        @RequestParam(defaultValue = "10") int pageSize) {
    SearchResponse data = tableAuditService
            .sourceWithAuditHistory(tableName, pageNo, pageSize);
    return ResponseEntity.ok(ApiResponse.success(data, REQUEST_SUCCESSFUL));
}
```

Centralize paths, defaults, messages, SQL templates, and limits instead of duplicating literals.

## 5. Response data contract

`SearchResponse` contains only:

- `pageNo`
- `pageSize`
- `numberOfElements`
- `totalElements`
- `totalPages`
- `hasPrevious`
- `hasNext`
- `rows`

Each row contains only:

- `id`
- `originalRecordPresent`
- `originalData`
- `changeSummary`
- `auditHistory`

`changeSummary` contains:

- `totalRevisions`
- `insertCount`
- `updateCount`
- `deleteCount`
- `unknownCount`
- `firstRevision`
- `latestRevision`

Each history item contains the derived fields below followed by every physical audit-table column:

- `sequenceNumber`
- `revision`
- `revisionTypeCode`
- `operation`
- Dynamic columns including `ID`, `REV`, `REVTYPE`, and all other selected columns

Use a map-backed immutable DTO with safe Jackson flattening such as `@JsonAnyGetter`. Never reduce
dynamic rows to a fixed projection.

State rules:

| Entity state | Required representation |
|---|---|
| Source row exists | `originalRecordPresent=true`; `originalData` is the complete current row |
| Source row deleted | `originalRecordPresent=false`; `originalData=null`; history remains complete |
| No revisions | Empty `auditHistory`; zero-valued `changeSummary` |
| Unknown `REVTYPE` | Preserve revision; `operation=UNKNOWN`; increment `unknownCount` |

## 6. Common API envelope

Every controller returns endpoint data only under `data`. Implement shared response metadata once:

- `BaseApiResponse<C>`: `timestamp`, `status`, `code`, `message`
- `ApiResponse<T>`: successful `data`
- `ApiError`: `data=null` and error-only fields

Success example:

```json
{
  "timestamp": "2026-09-14T00:15:30.412Z",
  "status": 200,
  "code": "SUCCESS",
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

The physical fields in this example are illustrative. At runtime return all columns actually
selected from Oracle. A deleted row keeps the same shape but uses
`originalRecordPresent=false`, `originalData=null`, and retains the complete history including its
`operation=DELETE` revision.

Success responses must not contain `sourceTable`, `auditTable`, `traceId`, `path`, or
`processingTimeMs`. Source and audit names remain internal query metadata. Use HTTP/body status
`200` for successful reads, updates, and deletes and `201` for creates. Internally map success to
support code `1000`, but expose client code `SUCCESS`.

## 7. Table-list endpoint

Create an `AuditableTable` enum containing only approved original table names and labels. Derive
audit names; do not store them in the enum. Initial mappings:

- `PMC_HOLIDAY_CALENDAR` -> `Holiday Calendar`
- `PMC_LOCO_SINGAPORE` -> `Loco Singapore`
- `PMC_POSITION_BALANCE` -> `Position Balance`

Implement in the same controller:

```http
GET /api/v1/allTable
```

It has no body and no pagination. Return labels under `data.tableLabels` using the common success
envelope. A URL-encoded returned label must also resolve in the generic audit endpoint.

## 8. Dynamic SQL, metadata, and performance

Apply all safeguards:

1. Resolve names through `AuditableTable` with a precomputed immutable lookup.
2. Normalize with `Locale.ROOT` and validate a strict simple Oracle identifier.
3. Reject names ending in the audit suffix.
4. Require the source in an external allowlist.
5. Derive the audit name internally.
6. Verify both tables and required columns through Oracle metadata.
7. Cache only successfully verified immutable descriptors.
8. Bind pagination and ID values; never concatenate caller-controlled identifiers.
9. Keep SQL templates in one SQL builder and avoid duplicated literals.

Optimized warm-request flow:

1. Build distinct IDs from the union of source and audit IDs.
2. Page them deterministically using Oracle offset/fetch and return the unpaged total in the same
   query using `COUNT(*) OVER()`.
3. Fetch all current rows for the page with one query.
4. Fetch all audit rows for the page with one query ordered by `ID, REV`.
5. Group histories and accumulate operation counts in one Java pass.

Normal populated pages therefore use three database queries and no N+1 reads. Empty first pages
skip source/history reads. An out-of-range page uses one fallback count because an empty Oracle
window page has no total value. Load required columns for both tables in one metadata query and
cache verified descriptors, never source/audit row data.

Use an immutable, null-safe `LinkedHashMap` snapshot because `Map.copyOf` rejects database nulls
and need not preserve column order. Recommend an audit index beginning with `(ID, REV)`, subject to
DBA review and actual Oracle execution plans.

Configure limits:

```yaml
audit-api:
  max-page-size: ${AUDIT_MAX_PAGE_SIZE:200}
```

Centralize `ORACLE_IN_LIMIT = 1000`. Require the configured maximum to be positive and no greater
than the Oracle limit. The lower default bounds response size, heap, connection occupancy,
serialization cost, and latency because one ID can expand into many revisions.

## 9. Errors and trace IDs

Use centralized `@RestControllerAdvice`. Every error contains:

- `timestamp`, HTTP `status`, stable `code`, safe `message`
- `data: null`
- `traceId`
- standard HTTP `error`
- non-null `details` array

Example:

```json
{
  "timestamp": "2026-09-14T00:15:30.412Z",
  "status": 500,
  "code": "DATABASE_ERROR",
  "message": "The database query could not be completed",
  "data": null,
  "traceId": "9f2c50d9bf5c71e5ad43b17ad84cb267",
  "error": "Internal Server Error",
  "details": []
}
```

Return validation problems together in `details`. Never expose request paths, SQL, Oracle details,
credentials, stack traces, or internal class names.

Support these stable categories: invalid identifier, page number, page size, request, validation,
audit-table input, unauthorized API key, disallowed table, missing table pair, missing required
column, missing Holiday, database failure, and unexpected failure. The unexpected fallback uses
client code `INTERNAL_ERROR` and internal support code `0000` in logs.

Generate trace IDs from at least 128 bits with `SecureRandom`, not UUID, encoded as lowercase hex.
Put the value in MDC, include it in error logs, return it in the error JSON and `X-Trace-Id` header,
and clear MDC in `finally`. Accept caller trace IDs only after strict safe-format validation.

Avoid nullable dereference warnings in exception handlers. Store nullable results locally and
check them before accessing their properties.

## 10. Optional API-key protection

Support configurable `X-API-Key` protection for `/api/v1/*`, disabled by default locally. Compare
keys in constant time. Return the common `401 UNAUTHORIZED` envelope for missing/invalid keys and
never log either key.

## 11. Typed Holiday JPA example

Retain a small typed JPA example proving JPA and JDBC coexist:

```http
GET    /api/v1/holidays?pageNo=0&pageSize=10
POST   /api/v1/holidays
PUT    /api/v1/holidays/{id}
DELETE /api/v1/holidays/{id}
```

Use validated request/response DTOs. Never expose the JPA entity from the controller. Do not add
runtime seed data or create production Oracle tables. Production schema generation stays disabled.

## 12. Configuration and code quality

- Read Oracle secrets from environment variables or an enterprise secret manager.
- Centralize API paths, defaults, messages, codes, headers, SQL, suffixes, and limits.
- Prefer constructor injection, focused classes, immutable DTOs, and read-only transactions.
- Keep dynamic source/audit columns inside maps; do not create per-table response types.
- Handle integer overflow in pagination offsets.
- Do not swallow exceptions or leak sensitive implementation details.
- Fix Sonar findings rather than suppressing them without justification.
- Remove unused imports, dead abstractions, duplicated strings, dummy data, and `.DS_Store` files.
- Keep `.DS_Store` ignored.
- Update `README.md`, `docs/PRD.md`, and this `promot.md` whenever the contract changes.

## 13. Tests and verification

Create tests in the matching production packages. Cover:

- Controller success envelopes, table labels, validation, and absence of success trace/time/path
- Service grouping, ordering, summary counts, deleted IDs, empty pages, and full row preservation
- DAO window-count pagination, out-of-range fallback, bindings, row mapping, and metadata batching
- Resolver allowlisting, suffix rejection, required columns, and successful descriptor caching
- `REVTYPE` mapping including unknown values
- Nullable dynamic columns and immutable response snapshots
- Error mapping, all validation details, safe messages, and error-only trace IDs
- `SecureRandom` trace format, MDC cleanup, and optional API-key behavior
- Typed Holiday controller, service, repository, entity, and validation behavior

Run:

```bash
mvn clean verify
```

The source and runtime baseline is Java 21. Maven may run on a compatible newer JDK, but compiler
output must target `--release 21`. Check `java -version` and `mvn -version` before diagnosing
compilation failures. Use `mvn -o clean verify` only when dependencies are already cached. If
Maven passes but an IDE is red, configure its project language level for Java 21, reload `pom.xml`,
and rebuild. Do not treat Maven/JDK dependency warnings as Java source compilation errors.

Configure JaCoCo in Maven to generate `target/site/jacoco/index.html` and fail `verify` unless both
line and branch coverage are 100%. Exclude only the Spring Boot launcher containing the
framework-delegating `main` method; do not exclude business, controller, service, DAO, validation,
security, tracing, exception, or DTO code to inflate the result.

The task is complete only when compilation succeeds, every test passes, documentation matches the
implemented response, no runtime dummy data exists, no `.DS_Store` remains, and the existing public
API contract is preserved unless a change was explicitly required.
