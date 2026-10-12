package integration

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	admindata "example.com/coupleapp/backend/internal/features/admin/data"
	admintransport "example.com/coupleapp/backend/internal/features/admin/transport"
	"example.com/coupleapp/backend/internal/platform/firebaseauth"
	"example.com/coupleapp/backend/internal/platform/httpapi"
	"firebase.google.com/go/v4/auth"
)

func TestAdminMetadataIsMinimalAndEveryLookupIsAudited(t *testing.T) {
	a, db := emulator(t)
	actorUID, actorToken := guest(t, a, db)
	targetUID, _ := guest(t, a, db)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	if err := a.SetCustomUserClaims(ctx, actorUID, map[string]any{"admin": true}); err != nil {
		t.Fatal(err)
	}
	if _, err := a.UpdateUser(ctx, targetUID, (&auth.UserToUpdate{}).Email("private-target@example.com").DisplayName("Private Target")); err != nil {
		t.Fatal(err)
	}
	owner := db.Collection("users").Doc(targetUID)
	if _, err := owner.Collection("counters").Doc("primary").Set(ctx, map[string]any{"version": int64(7), "payload": map[string]any{"DisplayName": "Private counter content"}}); err != nil {
		t.Fatal(err)
	}
	if _, err := owner.Collection("deletionRequests").Doc("current").Set(ctx, map[string]any{"status": "requested"}); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		cleanupCtx, cleanupCancel := context.WithTimeout(context.Background(), 20*time.Second)
		defer cleanupCancel()
		for _, collection := range []string{"counters", "deletionRequests"} {
			docs, _ := owner.Collection(collection).Documents(cleanupCtx).GetAll()
			for _, doc := range docs {
				_, _ = doc.Ref.Delete(cleanupCtx)
			}
		}
		docs, _ := db.Collection("adminAudit").Where("targetUid", "==", targetUID).Documents(cleanupCtx).GetAll()
		for _, doc := range docs {
			_, _ = doc.Ref.Delete(cleanupCtx)
		}
	})
	verifier := firebaseauth.Verifier{Client: a}
	handler := httpapi.NewRouter(nil, nil, admintransport.MetadataHandler{
		Auth:  verifier,
		Store: admindata.MetadataRepository{Auth: a, Firestore: db},
		Audit: admindata.AuditRepository{Firestore: db},
	})
	lookup := func(uid string) *httptest.ResponseRecorder {
		r := httptest.NewRequest(http.MethodGet, "/v1/admin/users/"+uid+"/metadata", nil)
		r.Header.Set("Authorization", "Bearer "+actorToken)
		w := httptest.NewRecorder()
		handler.ServeHTTP(w, r)
		return w
	}
	w := lookup(targetUID)
	if w.Code != 200 {
		t.Fatalf("status=%d body=%s", w.Code, w.Body.String())
	}
	for _, private := range []string{"private-target@example.com", "Private Target", "Private counter content"} {
		if strings.Contains(w.Body.String(), private) {
			t.Fatalf("private content leaked: %q", private)
		}
	}
	var body struct {
		UID                   string `json:"uid"`
		Anonymous             bool   `json:"anonymous"`
		CounterExists         bool   `json:"counterExists"`
		CounterVersion        int64  `json:"counterVersion"`
		DeletionRequestStatus string `json:"deletionRequestStatus"`
	}
	if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil {
		t.Fatal(err)
	}
	if body.UID != targetUID || !body.Anonymous || !body.CounterExists || body.CounterVersion != 7 || body.DeletionRequestStatus != "requested" {
		t.Fatalf("metadata=%+v", body)
	}
	missingUID := "missing-metadata-user"
	if w := lookup(missingUID); w.Code != 404 {
		t.Fatalf("missing status=%d", w.Code)
	}
	audits, err := db.Collection("adminAudit").Where("actorUid", "==", actorUID).Documents(ctx).GetAll()
	if err != nil {
		t.Fatal(err)
	}
	outcomes := map[string]bool{}
	for _, doc := range audits {
		data := doc.Data()
		target, _ := data["targetUid"].(string)
		outcome, _ := data["outcome"].(string)
		if target == targetUID || target == missingUID {
			outcomes[target+":"+outcome] = true
			t.Cleanup(func() { _, _ = doc.Ref.Delete(context.Background()) })
		}
	}
	if !outcomes[targetUID+":success"] || !outcomes[missingUID+":not_found"] {
		t.Fatalf("audit outcomes=%v", outcomes)
	}
	if err := a.SetCustomUserClaims(ctx, actorUID, map[string]any{"admin": false}); err != nil {
		t.Fatal(err)
	}
	if w := lookup(targetUID); w.Code != 403 {
		t.Fatalf("revoked admin status=%d", w.Code)
	}
}
