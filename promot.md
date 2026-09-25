# Portable Build Prompt: Enterprise Audit History API

Copy this entire file into a coding assistant to create or update the backend. Treat it as the
authoritative implementation prompt.

---

Act as a senior Java software engineer and solution designer. Build a production-quality backend
named **Enterprise Audit History API**. Inspect an existing project before changing it, preserve
correct behavior, remove obsolete code, and implement only the requirements below. For a new
project, create `enterprise-audit-history-api` using Maven.

## 1. Goal and scope

Create a reusable, read-focused Spring Boot API that returns complete current rows from dynamic
Oracle source tables together with complete history when their audit tables exist. One generic
endpoint must support audited and source-only tables without a separate entity, repository,
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

Use clear packages for configuration, controller, service, DAO, DTOs, entities, enums, resolvers,
validation, centralized exception handling, and the typed Holiday example. Keep tests in matching
packages.

Apply maintainable boundaries and SOLID principles:

- Keep controllers limited to HTTP mapping and response wrapping.
- Define `TableAuditService` as the controller-facing contract and keep `TableAuditServiceImpl`
  limited to validation, transaction/pagination orchestration, metadata resolution, and DAO
  coordination.
- Put source indexing, audit grouping, revision sequencing, and summary construction in a focused
  `AuditHistoryAssembler`.
- Hide database access behind DAO/JPA repository boundaries.
- Map Envers-compatible `REVTYPE` values centrally through `RevisionOperation` instead of
  branching in the service.
- Create the common error envelope centrally in `GlobalExceptionHandler`.
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

- Accept only a dynamically discovered source name or its generated public label.
- Reject an audit-table name passed directly.
- Use zero-based `pageNo`, default `0`.
- Use `pageSize`, default `10`.
- Validate `pageNo >= 0` and `1 <= pageSize <= configured maximum`.
- Derive the audit table with the configurable suffix, default `_AUD`.
- Page by distinct IDs across the union of source and audit tables when `_AUD` exists; otherwise
  page directly from the source table.
- Include audit-only IDs so deleted records remain discoverable.
- Fetch complete source and audit rows using `SELECT *`.
- When `_AUD` does not exist, return complete source rows with empty `auditHistory` and a zero-valued
  `changeSummary`. If `_AUD` exists but lacks required columns, return a configuration error.
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
- `approval`
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

`approval` must always contain `approvalRecordPresent`, `makerUsername`, and `checkerUsername`.
Discover only exact `<SOURCE>_APPROVAL_REQUEST` and `<SOURCE>_APPROVAL` tables. Do not support
override mappings or guess tables from partial or abbreviated names. A source must use exactly one
approval suffix, never both. Do not require an approval table to have its own `_AUD` companion when
enriching a source row with maker/checker usernames.
Apply scale-neutral numeric normalization only when matching a response entity
ID to an approval-table ID; do not change the existing source/audit grouping behavior. Then
load only `ID`, `MAKER_USERNAME`, and `CHECKER_USERNAME` in one page-level query. Do not parse or
return `PROPOSED_CHANGES`. Keep usernames for audit-only/deleted IDs when the approval row remains.

State rules:

| Entity state | Required representation |
|---|---|
| Source row exists | `originalRecordPresent=true`; `originalData` is the complete current row |
| Source row deleted | `originalRecordPresent=false`; `originalData=null`; history remains complete |
| No revisions | Empty `auditHistory`; zero-valued `changeSummary` |
| Unknown `REVTYPE` | Preserve revision; `operation=UNKNOWN`; increment `unknownCount` |

## 6. Common API envelope

Every controller returns endpoint data only under `data`. Implement shared response metadata once:

- `BaseApiResponse`: `timestamp`, application `status`, four-digit `code`, `message`
- `ApiResponse<T>`: successful `data`
- `ApiError`: `data=null` and error-only fields

Success example:

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
        "approval": {
          "approvalRecordPresent": true,
          "makerUsername": "maker.user",
          "checkerUsername": "checker.user"
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

Success responses must not contain `sourceTable`, `auditTable`, `path`, `httpStatus`, or
`processingTimeMs`. Source and audit names remain internal query metadata. Use HTTP `200` for
successful reads, updates, and deletes and HTTP `201` for creates. Return application
`status=SUCCESS` and `code=2000` in the body.

## 7. Dynamic table-list endpoint

Do not create a hardcoded table enum or external allowlist. Query Oracle `USER_TABLES` for source
tables beginning with the configurable prefix (default `PMC_`) and exclude tables ending with the
configurable audit suffix (default `_AUD`). Do not add an owner predicate and do not query
cross-schema `ALL_TABLES`.

Document that `USER_TABLES` discovers only objects owned by the connected Oracle user. The runtime
account must therefore connect as the owning schema with least privilege; cross-schema grants and
`CURRENT_SCHEMA` changes do not add objects to `USER_TABLES`.

Convert each source name to a label by removing the prefix, splitting on `_`, lower-casing with
`Locale.ROOT`, upper-casing the first character of each non-empty segment, and joining segments
with `-`. Example: `PMC_ACCOUNT_STATEMENT` becomes `Account-Statement`.

Implement in the same controller:

```http
GET /api/v1/allTable
```

It has no body and no pagination. Return labels in case-insensitive alphabetical order under
`data.tableLabels` using the common success envelope. A returned label, its physical source name,
and the legacy prefix-stripped underscore
name must resolve in the generic audit endpoint so existing clients remain compatible.

## 8. Dynamic SQL, metadata, and performance

Apply all safeguards:

1. Discover source names through a focused catalog backed by `USER_TABLES`.
2. Normalize with `Locale.ROOT` and validate a strict simple Oracle identifier.
3. Reject names ending in the audit suffix.
4. Require the source to be present in the current discovery result.
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
5. If an approval table exists, fetch ID/maker/checker for the page with one query.
6. Group histories, approvals, and operation counts in one Java pass.

Normal populated pages use three queries without approval or four with approval, and no N+1 reads.
Empty first pages skip source/history/approval reads. An out-of-range page uses one fallback count because an empty Oracle
window page has no total value. Load required columns for both tables in one metadata query and
cache verified descriptors, never row data.

Use an immutable, null-safe `LinkedHashMap` snapshot because `Map.copyOf` rejects database nulls
and need not preserve column order. Recommend an audit index beginning with `(ID, REV)`, subject to
DBA review and actual Oracle execution plans.

Configure limits:

```yaml
audit-api:
  source-table-prefix: ${AUDIT_SOURCE_TABLE_PREFIX:PMC_}
  max-page-size: ${AUDIT_MAX_PAGE_SIZE:200}
  approval:
    id-column: ID
    maker-username-column: MAKER_USERNAME
    checker-username-column: CHECKER_USERNAME
```

Centralize `ORACLE_IN_LIMIT = 1000`. Require the configured maximum to be positive and no greater
than the Oracle limit. The lower default bounds response size, heap, connection occupancy,
serialization cost, and latency because one ID can expand into many revisions.

## 9. Errors

Use centralized `@RestControllerAdvice`. Every error contains:

- `timestamp`, application `status`, stable four-digit `code`, safe `message`
- `data: null`
- standard HTTP `error`
- non-null `details` array

Example:

```json
{
  "timestamp": "2026-09-14T00:15:30.412Z",
  "status": "DATABASE_ERROR",
  "code": "5001",
  "message": "The database query could not be completed",
  "data": null,
  "error": "Internal Server Error",
  "details": []
}
```

Return validation problems together in `details`. Never expose request paths, SQL, Oracle details,
credentials, stack traces, or internal class names.

Use one `ApiOutcomeCode` enum for success and every error; do not create separate success/error code
enums. Each enum value is the JSON `status` and owns its four-digit JSON application `code`; the
status must identify the exact result represented by the message. Use `SUCCESS=2000`; reserve
`REDIRECTION=3000`; use client-error codes `INVALID_TABLE_NAME=4000`, `INVALID_PAGE_NO=4001`, `INVALID_PAGE_SIZE=4002`,
`AUDIT_TABLE_NOT_ACCEPTED=4003`, `VALIDATION_FAILED=4004`, `INVALID_REQUEST=4005`,
`TABLE_NOT_ALLOWED=4007`, `TABLE_PAIR_NOT_FOUND=4008`,
`MISSING_REQUIRED_COLUMN=4009`, `HOLIDAY_NOT_FOUND=4010`,
`COMMODITY_DEBIT_NOT_FOUND=4011`; and server-error codes
`INTERNAL_ERROR=5000`, `DATABASE_ERROR=5001`. Never repeat the numeric HTTP status in JSON.

Avoid nullable dereference warnings in exception handlers. Store nullable results locally and
check them before accessing their properties.

## 10. Typed Holiday JPA example

Retain a small typed JPA example proving JPA and JDBC coexist:

```http
GET    /api/v1/holidays?pageNo=0&pageSize=10
POST   /api/v1/holidays
PUT    /api/v1/holidays/{id}
DELETE /api/v1/holidays/{id}
```

Use validated request/response DTOs. Never expose the JPA entity from the controller. Do not add
runtime seed data or create production Oracle tables. Production schema generation stays disabled.

Generate and maintain one canonical `docs/API_RESPONSE_EXAMPLES.md` containing the request and
complete success response for every endpoint, plus the full status/code matrix and representative
validation, table/schema, database, and unexpected-error responses. Link it from
README and PRD; do not let duplicated examples contradict it.

## 11. Configuration and code quality

### Commodity debit PDF report

Implement `GET /api/v1/reports/commodity-debits/{id}/pdf` as a JPA-backed download from
`PMC_COMMODITY_DEBIT`. Use iText to generate A4 portrait output in memory and return
`application/pdf` with the exact attachment filename `community.pdf`. Keep the implementation
small: entity, repository, immutable DTO, reusable `PdfGenerationService`, report-specific
`CommodityDebitPdfTemplate`, coordinating service, controller, and typed not-found exception.
Do not add a mapper or template interface unless a second report proves it necessary. Render null
database columns as blanks and keep all report values database-backed; only labels and layout are
static. Preserve the standard JSON error envelope for missing data, database failures, and
unexpected PDF failures. Confirm iText licensing is appropriate for the deployment.

- Read Oracle secrets from environment variables or an enterprise secret manager.
- Centralize API paths, defaults, messages, codes, headers, SQL, suffixes, and limits.
- Prefer constructor injection, focused classes, immutable DTOs, and read-only transactions.
- Keep dynamic source/audit columns inside maps; do not create per-table response types.
- Handle integer overflow in pagination offsets.
- Do not swallow exceptions or leak sensitive implementation details.
- Use Lombok `@Slf4j` for structured audit-flow logs. Log only validated table identifiers,
  pagination values, aggregate counts, application codes, and exception types. Never log row data,
  entity IDs, request bodies, SQL, bind values, credentials, connection strings, raw exception
  messages, or stack traces.
- Fix Sonar findings rather than suppressing them without justification.
- Remove unused imports, dead abstractions, duplicated strings, dummy data, and `.DS_Store` files.
- Keep `.DS_Store` ignored.
- Update `README.md`, `docs/PRD.md`, and this `promot.md` whenever the contract changes.

## 12. Tests and verification

Create tests in the matching production packages. Cover:

- Controller success envelopes, table labels, validation, and absence of time/path metadata
- Service grouping, ordering, summary counts, deleted IDs, empty pages, and full row preservation
- DAO window-count pagination, out-of-range fallback, bindings, row mapping, and metadata batching
- Catalog discovery and label resolution, suffix rejection, required columns, and successful
  descriptor caching
- `REVTYPE` mapping including unknown values
- Nullable dynamic columns and immutable response snapshots
- Error mapping, all validation details, and safe messages
- Full-context MockMvc coverage proving public endpoints and the common error envelope end to end
- Typed Holiday controller, service, repository, entity, and validation behavior
- Commodity debit JPA mapping, A4 PDF content, null handling, download headers, missing-row error,
  and a full-context MockMvc download

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
exception, or DTO code to inflate the result.

The task is complete only when compilation succeeds, every test passes, documentation matches the
implemented response, no runtime dummy data exists, no `.DS_Store` remains, and the existing public
API contract is preserved unless a change was explicitly required.
