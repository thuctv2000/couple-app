# Counter sync v1 — implementation in progress

Go implements `GET /v1/me/counter` and `PUT /v1/me/counter`. Android debug now sends local operations through native Anonymous and the KMP HTTP adapter; offline edit/reopen/reconnect passed against local emulators on 11/10/2026. iOS and a real cloud project are not configured or verified.

## Identity and storage

- Bearer Firebase ID token, verified by Firebase Admin including revocation. UID comes only from the verified token; the body cannot select an owner.
- Counter: `users/{uid}/counters/primary`; receipts: `users/{uid}/counterOperations/{operationId}`.
- Firestore transaction reads both documents before writes; counter + receipt commit together. A replay returns the original operation ACK, even if a later edit already exists. A replay does not overwrite current state.
- Firestore client rules deny direct mobile access. Rules file is committed but not deployed. Server access uses IAM, not these client rules.
- No automatic receipt TTL yet. Account deletion must remove receipts as well as counter data before this can ship. A retention policy requires a protocol for stale retries; do not add TTL silently.

## PUT request

```json
{
  "operationId": "91cdbf3b-460f-4230-97d8-cd6df142f399",
  "baseVersion": 0,
  "payloadVersion": 1,
  "payload": {
    "displayName": "Linh",
    "partnerName": "Nam",
    "startDate": "2026-10-09",
    "timeZoneId": "Asia/Ho_Chi_Minh"
  }
}
```

Lowercase UUID operation ID; base version 0 means create-if-absent. Subsequent edits use the last acknowledged server version. Name limits match Kotlin's 80 UTF-16 code units, trimmed before queuing; future dates use the payload's explicit IANA zone. Body limit 8 KiB. Unknown fields and trailing JSON are rejected.

Success returns `{operationId, counter: {version, payload}, replay}`. GET returns `{version, payload}` or 404 when no cloud counter exists. Internal owner UID is omitted.

| Status / code | Client behavior |
|---|---|
| 400 invalid_request / invalid_operation | Keep local data, surface validation; do not retry endlessly |
| 401 unauthenticated | Refresh token with bounded retry; never change owner implicitly |
| 403 forbidden | Stop this scope's queue |
| 404 counter_not_found | GET has no cloud copy; first operation may use base 0 |
| 409 version_conflict | GET latest; preserve local and cloud copies for user decision |
| 409 operation_id_reused | Stop: same ID was reused with different content/base version |
| 503 service_unavailable | Retry with backoff; missing Firebase config also returns 503 |

## Mobile integration rules

Bind local owner to verified UID before sending. Freeze operation ID, base version and body once first sent; retries must send the same request. Do not blindly replay all local revisions at base 0: subsequent queued operations need acknowledged versions. Resolving a conflict creates a new operation, never changes a previously sent operation's content. ACK only removes its matching operation, not newer local edits.

## Setup and verification limits

Set `FIREBASE_PROJECT_ID` and use Application Default Credentials kept outside git. No key is created by this change. Without project config, health remains available and counter routes return 503. Emulators require `APP_ENV=local`, a `demo-*` project ID and both emulator hosts; mixed real/emulator configuration is rejected at startup.

Unit tests cover domain decisions and HTTP boundaries with test doubles. Local emulator integration tests additionally cover token verification, account isolation, deleted-user rejection, idempotent retry, concurrent writes and denial of direct Firestore client access. See [evidence](verification/go-emulator-tests.txt). Production credentials, IAM and mobile integration still need verification before M2.

Official references: [Firebase token verification](https://firebase.google.com/docs/auth/admin/verify-id-tokens), [Go Firestore transactions](https://pkg.go.dev/cloud.google.com/go/firestore#Client.RunTransaction).


## Conflict selection

A 409 version conflict triggers authenticated GET and stores its snapshot separately from local settings. The comparison includes both names, start date and time zone. User selection carries the displayed local revision and server version; the transaction rejects a stale selection or different UID. Choosing local replaces pending operations with a new ID based on that server version. Choosing server replaces local settings and clears that owner's queue. A subsequent server edit produces another 409 instead of being overwritten silently. Normal pull runs at foreground/manual sync and after draining queued writes. Snapshot application is transactional: ownership must match, no pending/conflicting local changes may exist, and the server version must advance (an identical snapshot at the current version is a no-op). Older versions or changed payloads at the same version are rejected. A local edit created while GET is in flight preserves its outbox and original cursor, then returns to push/conflict handling. A missing server counter does not erase local settings.

Android emulator runtime covers both selections and reopening after server selection. iOS has equivalent UI source and shared framework output, but native application build/runtime remains unverified. See [progress](phase-01-progress.md).


## Editor revisions

Editors retain the local revision at draft creation. Transactional compare-and-save rejects a changed revision with `edit_changed`, preserving the draft and creating no outbox item. Users can explicitly replace their draft with current saved data and continue editing. Never silently advance the expected revision of an old draft.
