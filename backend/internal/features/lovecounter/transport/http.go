package transport

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strings"

	"example.com/coupleapp/backend/internal/features/lovecounter/domain"
	"example.com/coupleapp/backend/internal/platform/identity"
)

type TokenVerifier interface {
	Verify(context.Context, string) (string, error)
}
type Repository interface {
	Get(context.Context, string) (domain.Counter, error)
	Apply(context.Context, string, domain.Operation) (domain.Decision, error)
}
type Handler struct {
	Auth  TokenVerifier
	Store Repository
}

func (h Handler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	if h.Auth == nil || h.Store == nil {
		fail(w, 503, "service_unavailable")
		return
	}
	parts := strings.Fields(r.Header.Get("Authorization"))
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
		fail(w, 401, "unauthenticated")
		return
	}
	uid, err := h.Auth.Verify(r.Context(), parts[1])
	if errors.Is(err, identity.ErrUnavailable) {
		fail(w, 503, "service_unavailable")
		return
	}
	if err != nil || uid == "" {
		fail(w, 401, "unauthenticated")
		return
	}
	if r.Method == http.MethodGet {
		counter, err := h.Store.Get(r.Context(), uid)
		if err != nil {
			respondError(w, err)
			return
		}
		_ = json.NewEncoder(w).Encode(counter)
		return
	}
	if r.Method != http.MethodPut {
		w.Header().Set("Allow", "GET, PUT")
		fail(w, 405, "method_not_allowed")
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 8192)
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()
	var request struct {
		ID             string          `json:"operationId"`
		BaseVersion    *int64          `json:"baseVersion"`
		PayloadVersion int             `json:"payloadVersion"`
		Payload        *domain.Payload `json:"payload"`
	}
	if err := decoder.Decode(&request); err != nil {
		fail(w, 400, "invalid_request")
		return
	}
	if err := decoder.Decode(new(any)); err != io.EOF {
		fail(w, 400, "invalid_request")
		return
	}
	if request.ID == "" || request.BaseVersion == nil || request.Payload == nil || request.PayloadVersion != 1 {
		fail(w, 400, "invalid_request")
		return
	}
	op := domain.Operation{ID: request.ID, BaseVersion: *request.BaseVersion, PayloadVersion: request.PayloadVersion, Payload: *request.Payload}
	decision, err := h.Store.Apply(r.Context(), uid, op)
	if err != nil {
		respondError(w, err)
		return
	}
	_ = json.NewEncoder(w).Encode(struct {
		OperationID string         `json:"operationId"`
		Counter     domain.Counter `json:"counter"`
		Replay      bool           `json:"replay"`
	}{op.ID, decision.Counter, decision.Replay})
}

func respondError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, domain.ErrInvalid):
		fail(w, 400, "invalid_operation")
	case errors.Is(err, domain.ErrConflict):
		fail(w, 409, "version_conflict")
	case errors.Is(err, domain.ErrOperationReused):
		fail(w, 409, "operation_id_reused")
	case errors.Is(err, domain.ErrScope):
		fail(w, 403, "forbidden")
	case errors.Is(err, domain.ErrNotFound):
		fail(w, 404, "counter_not_found")
	default:
		fail(w, 503, "service_unavailable")
	}
}
func fail(w http.ResponseWriter, status int, code string) {
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(map[string]string{"error": code})
}
