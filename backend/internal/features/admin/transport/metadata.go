package transport

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"strings"
	"time"

	admindomain "example.com/coupleapp/backend/internal/features/admin/domain"
	"example.com/coupleapp/backend/internal/platform/identity"
)

type MetadataStore interface {
	Get(context.Context, string) (admindomain.UserMetadata, error)
}
type AuditStore interface {
	Record(context.Context, admindomain.AuditEvent) error
}

type MetadataHandler struct {
	Auth  AdminVerifier
	Store MetadataStore
	Audit AuditStore
	Now   func() time.Time
}

func (h MetadataHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	fail := func(status int, code string) {
		w.WriteHeader(status)
		_ = json.NewEncoder(w).Encode(map[string]string{"error": code})
	}
	if r.Method != http.MethodGet {
		w.Header().Set("Allow", "GET")
		fail(405, "method_not_allowed")
		return
	}
	if h.Auth == nil || h.Store == nil || h.Audit == nil {
		fail(503, "service_unavailable")
		return
	}
	parts := strings.Fields(r.Header.Get("Authorization"))
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
		fail(401, "unauthenticated")
		return
	}
	actor, allowed, err := h.Auth.VerifyAdmin(r.Context(), parts[1])
	if errors.Is(err, identity.ErrUnavailable) {
		fail(503, "service_unavailable")
		return
	}
	if err != nil {
		fail(401, "unauthenticated")
		return
	}
	if !allowed {
		fail(403, "forbidden")
		return
	}
	target := r.PathValue("uid")
	if !validUID(target) {
		fail(400, "invalid_uid")
		return
	}
	metadata, lookupErr := h.Store.Get(r.Context(), target)
	outcome := "success"
	if errors.Is(lookupErr, admindomain.ErrNotFound) {
		outcome = "not_found"
	} else if lookupErr != nil {
		outcome = "unavailable"
	}
	now := time.Now
	if h.Now != nil {
		now = h.Now
	}
	if err := h.Audit.Record(r.Context(), admindomain.AuditEvent{ActorUID: actor, TargetUID: target, Action: "user_metadata_view", Outcome: outcome, OccurredAt: now()}); err != nil {
		fail(503, "service_unavailable")
		return
	}
	if errors.Is(lookupErr, admindomain.ErrNotFound) {
		fail(404, "user_not_found")
		return
	}
	if lookupErr != nil {
		fail(503, "service_unavailable")
		return
	}
	_ = json.NewEncoder(w).Encode(metadata)
}

func validUID(uid string) bool {
	return uid != "" && len(uid) <= 128 && uid != "." && uid != ".." && !strings.ContainsAny(uid, "/\\")
}
