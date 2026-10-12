package integration

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	admintransport "example.com/coupleapp/backend/internal/features/admin/transport"
	"example.com/coupleapp/backend/internal/platform/firebaseauth"
	"example.com/coupleapp/backend/internal/platform/httpapi"
)

func TestAdminRoleChangesApplyToExistingToken(t *testing.T) {
	a, db := emulator(t)
	uid, token := guest(t, a, db)
	_, otherToken := guest(t, a, db)
	handler := httpapi.NewRouter(nil, admintransport.SessionHandler{Auth: firebaseauth.Verifier{Client: a}}, nil)
	check := func(token string, want int) {
		t.Helper()
		r := httptest.NewRequest(http.MethodGet, "/v1/admin/session?admin=true", nil)
		r.Header.Set("Authorization", "Bearer "+token)
		r.Header.Set("X-Role", "admin")
		w := httptest.NewRecorder()
		handler.ServeHTTP(w, r)
		if w.Code != want {
			t.Fatalf("status=%d want=%d", w.Code, want)
		}
	}
	check("invalid", 401)
	check(token, 403)
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()
	if err := a.SetCustomUserClaims(ctx, uid, map[string]any{"admin": true}); err != nil {
		t.Fatal(err)
	}
	check(token, 200)
	check(otherToken, 403)
	if err := a.SetCustomUserClaims(ctx, uid, map[string]any{"admin": false}); err != nil {
		t.Fatal(err)
	}
	check(token, 403) // Same still-valid ID token must no longer authorize admin.
}
