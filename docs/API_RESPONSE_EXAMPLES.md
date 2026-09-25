# API Request and Response Examples

This document is the canonical request/response reference for Enterprise Audit History API.
Values are illustrative; source and audit column names come from Oracle at runtime.

For a larger complete-column example, see
[position-balance-success-response.json](examples/position-balance-success-response.json). The
application never reads this documentation file; actual response values always come from Oracle.

## Common contract

The numeric protocol status is returned by HTTP and is not repeated in JSON. Every body contains
an outcome-specific `status`, its stable four-digit application `code`, a safe `message`, and
endpoint content under `data`.

Error responses contain `error` and `details`; they never contain `path`, SQL, credentials, stack
traces, or Oracle messages.

## 1. List supported tables

```http
GET /api/v1/allTable
```

No request body or pagination is accepted.

HTTP `200`:

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

## 2. Read a source table with audit history

```http
GET /api/v1/Loco-Singapore?pageNo=0&pageSize=10
```

No request body is accepted. `pageNo` is zero-based; `pageSize` defaults to `10` and must not
exceed `audit-api.max-page-size`.

HTTP `200`:

```json
{
  "timestamp": "2026-09-14T04:31:00Z",
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

For a deleted entity, the same row has `originalRecordPresent: false`, `originalData: null`, and
retains its complete `auditHistory`, including the `DELETE` revision. Its retained approval row
still supplies maker/checker usernames. If no matching approval table or row exists, the row uses:

```json
"approval": {
  "approvalRecordPresent": false,
  "makerUsername": null,
  "checkerUsername": null
}
```

Approval row presence is independent of checker nullability. A pending database row can return:

```json
"approval": {
  "approvalRecordPresent": true,
  "makerUsername": "vengza",
  "checkerUsername": null
}
```

An approved row containing both database usernames returns:

```json
"approval": {
  "approvalRecordPresent": true,
  "makerUsername": "user1",
  "checkerUsername": "vengza"
}
```

A row containing only a checker still remains present:

```json
"approval": {
  "approvalRecordPresent": true,
  "makerUsername": null,
  "checkerUsername": "checker.user"
}
```

If the approval row exists with both username values null, presence remains true:

```json
"approval": {
  "approvalRecordPresent": true,
  "makerUsername": null,
  "checkerUsername": null
}
```

An empty result returns the
same pagination object with zero totals and `"rows": []`.

## 3. List holidays

```http
GET /api/v1/holidays?pageNo=0&pageSize=10
```

HTTP `200`:

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

## 4. Create a holiday

```http
POST /api/v1/holidays
Content-Type: application/json
```

```json
{
  "id": 101,
  "holidayDate": "2026-12-25",
  "calendarCode": "SG",
  "calendarName": "Singapore Calendar",
  "username": "operations-user"
}
```

HTTP `201`:

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
    "version": 0,
    "createdBy": "operations-user",
    "createdOn": "2026-09-14T12:33:00",
    "updatedBy": "operations-user",
    "updatedOn": "2026-09-14T12:33:00"
  }
}
```

## 5. Update a holiday

```http
PUT /api/v1/holidays/101
Content-Type: application/json
```

```json
{
  "id": 101,
  "holidayDate": "2026-12-26",
  "calendarCode": "SG",
  "calendarName": "Singapore Calendar Updated",
  "username": "operations-user"
}
```

HTTP `200` returns the updated `HolidayResponse`:

```json
{
  "timestamp": "2026-09-14T04:34:00Z",
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": {
    "id": 101,
    "holidayDate": "2026-12-26",
    "calendarCode": "SG",
    "calendarName": "Singapore Calendar Updated",
    "version": 1,
    "createdBy": "operations-user",
    "createdOn": "2026-09-14T12:33:00",
    "updatedBy": "operations-user",
    "updatedOn": "2026-09-14T12:34:00"
  }
}
```

The path ID identifies the record being updated. The request `id` remains validated but does not
replace the path ID.

## 6. Delete a holiday

```http
DELETE /api/v1/holidays/101
```

No request body is accepted. HTTP `200`:

```json
{
  "timestamp": "2026-09-14T04:35:00Z",
  "status": "SUCCESS",
  "code": "2000",
  "message": "Request completed successfully",
  "data": null
}
```

## 7. Download a commodity debit PDF

```http
GET /api/v1/reports/commodity-debits/1001/pdf
```

No request body is accepted. HTTP `200` is a binary response:

```http
Content-Type: application/pdf
Content-Disposition: attachment; filename="community.pdf"
Cache-Control: no-store
```

When ID `1001` is absent, HTTP `404` returns JSON:

```json
{
  "timestamp": "2026-09-25T10:00:00Z",
  "status": "COMMODITY_DEBIT_NOT_FOUND",
  "code": "4011",
  "message": "Commodity debit report data not found: 1001",
  "data": null,
  "error": "Not Found",
  "details": []
}
```

## Error scenarios

Every error uses its exact outcome status and corresponding application code:

| Scenario | HTTP | `status` | `code` | Message |
|---|---:|---|---:|---|
| Invalid table identifier | 400 | `INVALID_TABLE_NAME` | `4000` | `tableName must be a simple Oracle identifier` |
| Negative page number | 400 | `INVALID_PAGE_NO` | `4001` | `pageNo must be zero or greater` |
| Invalid page size | 400 | `INVALID_PAGE_SIZE` | `4002` | `pageSize must be between 1 and <maximum>` |
| Audit table used as input | 400 | `AUDIT_TABLE_NOT_ACCEPTED` | `4003` | `Pass the source table name, not the audit table name` |
| Request-body validation | 400 | `VALIDATION_FAILED` | `4004` | `Request validation failed` |
| Malformed request/type | 400 | `INVALID_REQUEST` | `4005` | `Invalid request` |
| Table not discovered | 404 | `TABLE_NOT_ALLOWED` | `4007` | Identifies the rejected table |
| Source/audit pair missing | 404 | `TABLE_PAIR_NOT_FOUND` | `4008` | Identifies the missing pair |
| Required column missing | 422 | `MISSING_REQUIRED_COLUMN` | `4009` | Identifies the table and column |
| Holiday not found | 404 | `HOLIDAY_NOT_FOUND` | `4010` | Identifies the holiday ID |
| Commodity debit report data missing | 404 | `COMMODITY_DEBIT_NOT_FOUND` | `4011` | Identifies the report row ID |
| Unexpected exception | 500 | `INTERNAL_ERROR` | `5000` | `An unexpected error occurred` |
| Oracle/JDBC failure | 500 | `DATABASE_ERROR` | `5001` | `The database query could not be completed` |

Invalid page example, HTTP `400`:

```json
{
  "timestamp": "2026-09-14T04:36:00Z",
  "status": "INVALID_PAGE_NO",
  "code": "4001",
  "message": "pageNo must be zero or greater",
  "data": null,
  "error": "Bad Request",
  "details": []
}
```

Request validation example, HTTP `400`:

```json
{
  "timestamp": "2026-09-14T04:37:00Z",
  "status": "VALIDATION_FAILED",
  "code": "4004",
  "message": "Request validation failed",
  "data": null,
  "error": "Bad Request",
  "details": [
    {
      "field": "calendarCode",
      "message": "must not be blank"
    },
    {
      "field": "holidayDate",
      "message": "must not be null"
    }
  ]
}
```

Database failure example, HTTP `500`:

```json
{
  "timestamp": "2026-09-14T04:38:00Z",
  "status": "DATABASE_ERROR",
  "code": "5001",
  "message": "The database query could not be completed",
  "data": null,
  "error": "Internal Server Error",
  "details": []
}
```
