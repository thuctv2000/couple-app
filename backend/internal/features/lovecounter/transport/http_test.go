package transport

import (
	"context"
	"errors"
	"net/http/httptest"
	"strings"
	"testing"

	"example.com/coupleapp/backend/internal/features/lovecounter/domain"
	"example.com/coupleapp/backend/internal/platform/identity"
)

type verifierFunc func(context.Context, string) (string, error)

func TestIdentityOutageDoesNotForceReauthentication(t *testing.T) {
	s := &testStore{}
	h := Handler{Auth: verifierFunc(func(context.Context, string) (string, error) { return "", identity.ErrUnavailable }), Store: s}
	r := httptest.NewRequest("GET", "/v1/me/counter", nil)
	r.Header.Set("Authorization", "Bearer token")
	w := httptest.NewRecorder()
	h.ServeHTTP(w, r)
	if w.Code != 503 || s.calls != 0 {
		t.Fatalf("status=%d calls=%d", w.Code, s.calls)
	}
}

func (f verifierFunc) Verify(ctx context.Context, token string) (string, error) { return f(ctx, token) }

type testStore struct {
	uid   string
	calls int
	err   error
}

func (s *testStore) Get(_ context.Context, uid string) (domain.Counter, error) {
	s.uid = uid
	s.calls++
	return domain.Counter{OwnerUID: uid, Version: 1}, s.err
}
func (s *testStore) Apply(_ context.Context, uid string, _ domain.Operation) (domain.Decision, error) {
	s.uid = uid
	s.calls++
	return domain.Decision{Counter: domain.Counter{OwnerUID: uid, Version: 1}}, s.err
}

func TestRejectsInvalidIdentityBeforeStorage(t *testing.T) {
	for _, header := range []string{"", "Basic token", "Bearer", "Bearer bad-token"} {
		s := &testStore{}
		h := Handler{Auth: verifierFunc(func(context.Context, string) (string, error) { return "", errors.New("invalid") }), Store: s}
		r := httptest.NewRequest("GET", "/v1/me/counter", nil)
		r.Header.Set("Authorization", header)
		w := httptest.NewRecorder()
		h.ServeHTTP(w, r)
		if w.Code != 401 || s.calls != 0 {
			t.Fatalf("%q: status=%d calls=%d", header, w.Code, s.calls)
		}
	}
}

func TestOnlyVerifiedUIDReachesStorage(t *testing.T) {
	s := &testStore{}
	h := Handler{Auth: verifierFunc(func(context.Context, string) (string, error) { return "verified-user", nil }), Store: s}
	r := httptest.NewRequest("GET", "/v1/me/counter?uid=attacker", nil)
	r.Header.Set("Authorization", "Bearer valid-test-token")
	w := httptest.NewRecorder()
	h.ServeHTTP(w, r)
	if w.Code != 200 || s.uid != "verified-user" {
		t.Fatalf("status=%d uid=%s", w.Code, s.uid)
	}
	if strings.Contains(w.Body.String(), "verified-user") {
		t.Fatal("internal UID leaked into response")
	}
}

func TestStrictBodyRejectsClientOwnerAndTrailingJSON(t *testing.T) {
	for _, body := range []string{`{"ownerUID":"attacker"}`, `{} {}`, strings.Repeat("x", 9000)} {
		s := &testStore{}
		h := Handler{Auth: verifierFunc(func(context.Context, string) (string, error) { return "verified-user", nil }), Store: s}
		r := httptest.NewRequest("PUT", "/v1/me/counter", strings.NewReader(body))
		r.Header.Set("Authorization", "Bearer valid-test-token")
		w := httptest.NewRecorder()
		h.ServeHTTP(w, r)
		if w.Code != 400 || s.calls != 0 {
			t.Fatalf("status=%d calls=%d", w.Code, s.calls)
		}
	}
}

func TestErrorsAreSafeAndActionable(t *testing.T) {
	for _, tc := range []struct {
		err    error
		status int
		code   string
	}{
		{domain.ErrConflict, 409, "version_conflict"},
		{domain.ErrOperationReused, 409, "operation_id_reused"},
		{domain.ErrScope, 403, "forbidden"},
		{domain.ErrNotFound, 404, "counter_not_found"},
		{errors.New("private infrastructure detail"), 503, "service_unavailable"},
	} {
		s := &testStore{err: tc.err}
		h := Handler{Auth: verifierFunc(func(context.Context, string) (string, error) { return "verified-user", nil }), Store: s}
		r := httptest.NewRequest("GET", "/v1/me/counter", nil)
		r.Header.Set("Authorization", "Bearer valid-test-token")
		w := httptest.NewRecorder()
		h.ServeHTTP(w, r)
		if w.Code != tc.status || !strings.Contains(w.Body.String(), tc.code) || strings.Contains(w.Body.String(), "private") {
			t.Fatal(w.Code, w.Body.String())
		}
	}
	response := httptest.NewRecorder()
	Handler{}.ServeHTTP(response, httptest.NewRequest("GET", "/v1/me/counter", nil))
	if response.Code != 503 {
		t.Fatal(response.Code)
	}
}
