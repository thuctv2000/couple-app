// Package integration runs only against explicitly configured local emulators.
package integration

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"sync"
	"testing"
	"time"

	"cloud.google.com/go/firestore"
	"example.com/coupleapp/backend/internal/features/lovecounter/data"
	"example.com/coupleapp/backend/internal/features/lovecounter/domain"
	"example.com/coupleapp/backend/internal/features/lovecounter/transport"
	"example.com/coupleapp/backend/internal/platform/firebaseauth"
	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/auth"
	"github.com/google/uuid"
)

func emulator(t *testing.T) (*auth.Client, *firestore.Client) {
	t.Helper()
	for _, name := range []string{"FIREBASE_AUTH_EMULATOR_HOST", "FIRESTORE_EMULATOR_HOST"} {
		value := os.Getenv(name)
		if value == "" {
			t.Skip("local Auth and Firestore emulators required")
		}
		host, _, err := net.SplitHostPort(value)
		ip := net.ParseIP(host)
		if err != nil || ip == nil || !ip.IsLoopback() {
			t.Fatal("integration tests require loopback emulator hosts")
		}
	}
	ctx := context.Background()
	app, err := firebase.NewApp(ctx, &firebase.Config{ProjectID: "demo-couple-app"})
	if err != nil {
		t.Fatal(err)
	}
	a, err := app.Auth(ctx)
	if err != nil {
		t.Fatal(err)
	}
	db, err := app.Firestore(ctx)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = db.Close() })
	return a, db
}

func guest(t *testing.T, a *auth.Client, db *firestore.Client) (uid, token string) {
	t.Helper()
	client := &http.Client{Timeout: 10 * time.Second}
	url := "http://" + os.Getenv("FIREBASE_AUTH_EMULATOR_HOST") + "/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo-key"
	response, err := client.Post(url, "application/json", strings.NewReader(`{"returnSecureToken":true}`))
	if err != nil {
		t.Fatal(err)
	}
	defer response.Body.Close()
	if response.StatusCode != 200 {
		t.Fatalf("guest sign-up status %d", response.StatusCode)
	}
	var result struct {
		LocalID string `json:"localId"`
		IDToken string `json:"idToken"`
	}
	if err := json.NewDecoder(response.Body).Decode(&result); err != nil {
		t.Fatal(err)
	}
	if result.LocalID == "" || result.IDToken == "" {
		t.Fatal("guest response missing identity")
	}
	t.Cleanup(func() {
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		owner := db.Collection("users").Doc(result.LocalID)
		for _, name := range []string{"counters", "counterOperations"} {
			docs, err := owner.Collection(name).Documents(ctx).GetAll()
			if err != nil {
				t.Errorf("cleanup query: %v", err)
				continue
			}
			for _, doc := range docs {
				if _, err := doc.Ref.Delete(ctx); err != nil {
					t.Errorf("cleanup delete: %v", err)
				}
			}
		}
		_ = a.DeleteUser(ctx, result.LocalID)
	})
	return result.LocalID, result.IDToken
}

func request(h http.Handler, method, token string, op *domain.Operation) *httptest.ResponseRecorder {
	var body bytes.Buffer
	if op != nil {
		_ = json.NewEncoder(&body).Encode(op)
	}
	r := httptest.NewRequest(method, "/v1/me/counter", &body)
	r.Header.Set("Authorization", "Bearer "+token)
	w := httptest.NewRecorder()
	h.ServeHTTP(w, r)
	return w
}

func TestFirebaseIdentityAndAtomicReplay(t *testing.T) {
	a, db := emulator(t)
	uid, token := guest(t, a, db)
	_, otherToken := guest(t, a, db)
	h := transport.Handler{Auth: firebaseauth.Verifier{Client: a}, Store: data.FirestoreRepository{Client: db}}
	op := domain.Operation{ID: uuid.NewString(), PayloadVersion: 1, Payload: domain.Payload{DisplayName: "Linh", StartDate: "2020-01-01", TimeZoneID: "Asia/Ho_Chi_Minh"}}
	for _, tc := range []struct {
		method, token string
		op            *domain.Operation
		status        int
	}{
		{"GET", "invalid-token", nil, 401}, {"GET", token, nil, 404},
		{"PUT", token, &op, 200}, {"PUT", token, &op, 200}, {"GET", otherToken, nil, 404},
	} {
		w := request(h, tc.method, tc.token, tc.op)
		if w.Code != tc.status {
			t.Fatalf("%s status %d, want %d: %s", tc.method, w.Code, tc.status, w.Body.String())
		}
	}
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()
	repo := data.FirestoreRepository{Client: db}
	stored, err := repo.Get(ctx, uid)
	if err != nil || stored.Version != 1 || stored.Payload != op.Payload {
		t.Fatalf("stored %+v, %v", stored, err)
	}
	op.Payload.DisplayName = "Same operation changed"
	if w := request(h, "PUT", token, &op); w.Code != 409 {
		t.Fatal(w.Code, w.Body.String())
	}
	// The same client token may use Go, but must not bypass it via Firestore REST.
	for _, method := range []string{"GET", "PATCH"} {
		url := "http://" + os.Getenv("FIRESTORE_EMULATOR_HOST") + "/v1/projects/demo-couple-app/databases/(default)/documents/users/" + uid + "/counters/primary"
		r, err := http.NewRequestWithContext(ctx, method, url, strings.NewReader(`{"fields":{}}`))
		if err != nil {
			t.Fatal(err)
		}
		r.Header.Set("Authorization", "Bearer "+token)
		r.Header.Set("Content-Type", "application/json")
		response, err := (&http.Client{Timeout: 10 * time.Second}).Do(r)
		if err != nil {
			t.Fatal(err)
		}
		response.Body.Close()
		if response.StatusCode != 403 {
			t.Fatalf("direct Firestore %s status=%d, want 403", method, response.StatusCode)
		}
	}
	if err := a.DeleteUser(ctx, uid); err != nil {
		t.Fatal(err)
	}
	if w := request(h, "GET", token, nil); w.Code != 401 {
		t.Fatal("deleted user's token accepted", w.Code)
	}
}

func TestConcurrentWritesHaveOneWinnerAndOneReceipt(t *testing.T) {
	a, db := emulator(t)
	uid, _ := guest(t, a, db)
	repo := data.FirestoreRepository{Client: db}
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	var wg sync.WaitGroup
	results := make(chan error, 4)
	for i := 0; i < 4; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			op := domain.Operation{ID: uuid.NewString(), PayloadVersion: 1, Payload: domain.Payload{DisplayName: fmt.Sprint(i), StartDate: "2020-01-01", TimeZoneID: "UTC"}}
			_, err := repo.Apply(ctx, uid, op)
			results <- err
		}(i)
	}
	wg.Wait()
	close(results)
	winners := 0
	for err := range results {
		if err == nil {
			winners++
		} else if err != domain.ErrConflict {
			t.Fatal(err)
		}
	}
	if winners != 1 {
		t.Fatalf("got %d winners", winners)
	}
	stored, err := repo.Get(ctx, uid)
	if err != nil || stored.Version != 1 {
		t.Fatalf("counter %+v, %v", stored, err)
	}
	receipts, err := db.Collection("users").Doc(uid).Collection("counterOperations").Documents(ctx).GetAll()
	if err != nil || len(receipts) != 1 {
		t.Fatalf("receipts=%d, %v", len(receipts), err)
	}
}
