package transport

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"example.com/coupleapp/backend/internal/platform/identity"
)

type AdminVerifier interface {
	VerifyAdmin(context.Context, string) (string, bool, error)
}

type SessionHandler struct{ Auth AdminVerifier }

func (h SessionHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
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
	if h.Auth == nil {
		fail(503, "service_unavailable")
		return
	}
	parts := strings.Fields(r.Header.Get("Authorization"))
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
		fail(401, "unauthenticated")
		return
	}
	_, allowed, err := h.Auth.VerifyAdmin(r.Context(), parts[1])
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
	_ = json.NewEncoder(w).Encode(map[string]string{"role": "admin"})
}
