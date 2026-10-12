package transport

import (
	"context"
	"errors"
	"example.com/coupleapp/backend/internal/platform/identity"
	"net/http/httptest"
	"strings"
	"testing"
)

type verifier struct {
	allowed bool
	err     error
}

func (v verifier) VerifyAdmin(context.Context, string) (string, bool, error) {
	return "admin-uid", v.allowed, v.err
}
func TestAdminSessionAuthorization(t *testing.T) {
	for _, tc := range []struct {
		name, method, header string
		auth                 AdminVerifier
		code                 int
		body                 string
	}{
		{"missing configuration", "GET", "", nil, 503, "service_unavailable"},
		{"missing token", "GET", "", verifier{true, nil}, 401, "unauthenticated"},
		{"malformed token", "GET", "Bearer a b", verifier{true, nil}, 401, "unauthenticated"},
		{"invalid token", "GET", "Bearer token", verifier{false, errors.New("secret upstream error")}, 401, "unauthenticated"},
		{"ordinary user", "GET", "Bearer token", verifier{false, nil}, 403, "forbidden"},
		{"admin", "GET", "Bearer token", verifier{true, nil}, 200, "admin"},
		{"auth unavailable", "GET", "Bearer token", verifier{false, identity.ErrUnavailable}, 503, "service_unavailable"},
		{"no mutation", "POST", "Bearer token", verifier{true, nil}, 405, "method_not_allowed"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			request := httptest.NewRequest(tc.method, "/v1/admin/session?admin=true", nil)
			request.Header.Set("Authorization", tc.header)
			request.Header.Set("X-Role", "admin")
			response := httptest.NewRecorder()
			SessionHandler{Auth: tc.auth}.ServeHTTP(response, request)
			if response.Code != tc.code || !strings.Contains(response.Body.String(), tc.body) {
				t.Fatalf("response=%d %s", response.Code, response.Body.String())
			}
			if response.Header().Get("Cache-Control") != "no-store" {
				t.Fatal("admin response must not be cached")
			}
			if strings.Contains(response.Body.String(), "secret") {
				t.Fatal("error details leaked")
			}
		})
	}
}
