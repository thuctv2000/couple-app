package transport

import (
	"context"
	"errors"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	admindomain "example.com/coupleapp/backend/internal/features/admin/domain"
	"example.com/coupleapp/backend/internal/platform/identity"
)

type metadataVerifier struct {
	actor   string
	allowed bool
	err     error
}

func (v metadataVerifier) VerifyAdmin(context.Context, string) (string, bool, error) {
	return v.actor, v.allowed, v.err
}

type metadataStore struct {
	value     admindomain.UserMetadata
	err       error
	requested string
}

func (s *metadataStore) Get(_ context.Context, uid string) (admindomain.UserMetadata, error) {
	s.requested = uid
	return s.value, s.err
}

type auditStore struct {
	event admindomain.AuditEvent
	err   error
	calls int
}

func (s *auditStore) Record(_ context.Context, event admindomain.AuditEvent) error {
	s.calls++
	s.event = event
	return s.err
}

func TestMetadataRequiresCurrentAdminAndAuditsLookup(t *testing.T) {
	fixed := time.Date(2026, 10, 12, 7, 0, 0, 0, time.UTC)
	for _, tc := range []struct {
		name    string
		auth    AdminVerifier
		lookup  error
		audit   error
		status  int
		outcome string
	}{
		{"missing config", nil, nil, nil, 503, ""},
		{"invalid token", metadataVerifier{err: errors.New("private")}, nil, nil, 401, ""},
		{"identity unavailable", metadataVerifier{err: identity.ErrUnavailable}, nil, nil, 503, ""},
		{"ordinary user", metadataVerifier{actor: "actor", allowed: false}, nil, nil, 403, ""},
		{"success", metadataVerifier{actor: "actor", allowed: true}, nil, nil, 200, "success"},
		{"not found", metadataVerifier{actor: "actor", allowed: true}, admindomain.ErrNotFound, nil, 404, "not_found"},
		{"lookup unavailable", metadataVerifier{actor: "actor", allowed: true}, admindomain.ErrUnavailable, nil, 503, "unavailable"},
		{"audit unavailable", metadataVerifier{actor: "actor", allowed: true}, nil, admindomain.ErrUnavailable, 503, "success"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			store := &metadataStore{value: admindomain.UserMetadata{UID: "target", Providers: []string{}, DeletionRequestStatus: "none"}, err: tc.lookup}
			audit := &auditStore{err: tc.audit}
			handler := MetadataHandler{Auth: tc.auth, Store: store, Audit: audit, Now: func() time.Time { return fixed }}
			r := httptest.NewRequest("GET", "/v1/admin/users/target/metadata", nil)
			r.SetPathValue("uid", "target")
			r.Header.Set("Authorization", "Bearer token")
			w := httptest.NewRecorder()
			handler.ServeHTTP(w, r)
			if w.Code != tc.status {
				t.Fatalf("status=%d body=%s", w.Code, w.Body.String())
			}
			if w.Header().Get("Cache-Control") != "no-store" {
				t.Fatal("metadata must not cache")
			}
			if tc.outcome == "" {
				if audit.calls != 0 {
					t.Fatal("unauthorized request audited as support action")
				}
			} else {
				if audit.calls != 1 || audit.event.ActorUID != "actor" || audit.event.TargetUID != "target" || audit.event.Outcome != tc.outcome || !audit.event.OccurredAt.Equal(fixed) {
					t.Fatalf("bad audit: %+v", audit.event)
				}
			}
			if strings.Contains(w.Body.String(), "private") {
				t.Fatal("private error leaked")
			}
		})
	}
}

func TestMetadataRejectsInvalidUIDBeforeLookup(t *testing.T) {
	for _, uid := range []string{"", ".", "..", "a/b", `a\b`, strings.Repeat("x", 129)} {
		store := &metadataStore{}
		audit := &auditStore{}
		handler := MetadataHandler{Auth: metadataVerifier{actor: "actor", allowed: true}, Store: store, Audit: audit}
		r := httptest.NewRequest("GET", "/v1/admin/users/x/metadata", nil)
		r.SetPathValue("uid", uid)
		r.Header.Set("Authorization", "Bearer token")
		w := httptest.NewRecorder()
		handler.ServeHTTP(w, r)
		if w.Code != 400 || store.requested != "" || audit.calls != 0 {
			t.Fatalf("uid=%q status=%d", uid, w.Code)
		}
	}
}
