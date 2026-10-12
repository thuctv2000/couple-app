# Backend request logs

Current implementation: `internal/platform/httpapi/accesslog.go`, wired around the router in `cmd/api/main.go`.

Each completed HTTP request emits JSON via Go `slog` to stdout:

| Field | Meaning |
|---|---|
| `request_id` | Server-generated UUID, also returned in `X-Request-ID`; incoming IDs are not reused |
| `method` | Standard HTTP method, otherwise `OTHER` |
| `route` | Registered route pattern, or `unmatched`; never the raw URL |
| `status` | First final response status; implicit response is 200 |
| `duration_ms` | Elapsed handler time |
| `time`, `level`, `msg` | Logger metadata; event is `http_request` |

4xx emits WARN, 5xx ERROR, other responses INFO. The middleware forwards response bodies without logging them. Headers, query strings, raw paths, IPs, user IDs and tokens are excluded. Application handlers must maintain this rule for any future log events. A request ID is a diagnostic identifier, not an authentication credential.

Tests cover the returned request ID matching the event, caller ID rejection, first-status preservation, severity and unknown route/method handling. Inputs deliberately contain private markers in body, response, URL, headers and IP; none may appear in the log. `go test -race ./...` and `go vet ./...` passed on 12/10/2026; emulator integration tests were not enabled in this run. See `verification/go-access-log-tests.txt`.

Limits: this is a completed-request access log, not distributed tracing, panic reporting, metrics, analytics, an admin audit trail or log retention management. No collection service/project is created. Deployment must define stdout collection/access/retention; mobile crash and consent-aware analytics remain separate work.
