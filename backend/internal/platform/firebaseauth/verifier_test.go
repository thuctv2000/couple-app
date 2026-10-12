package firebaseauth

import (
	"context"
	"errors"
	"example.com/coupleapp/backend/internal/platform/identity"
	"firebase.google.com/go/v4/auth"
	"testing"
)

type fakeAuth struct {
	verified          *auth.Token
	user              *auth.UserRecord
	tokenErr, userErr error
	requestedUID      string
}

func (f *fakeAuth) VerifyIDTokenAndCheckRevoked(context.Context, string) (*auth.Token, error) {
	return f.verified, f.tokenErr
}
func (f *fakeAuth) GetUser(_ context.Context, uid string) (*auth.UserRecord, error) {
	f.requestedUID = uid
	return f.user, f.userErr
}
func TestAdminUsesCurrentServerClaims(t *testing.T) {
	for _, tc := range []struct {
		name     string
		claim    any
		disabled bool
		want     bool
	}{
		{"admin", true, false, true}, {"ordinary", nil, false, false}, {"removed", false, false, false}, {"string is not boolean", "true", false, false}, {"disabled", true, true, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			client := &fakeAuth{verified: &auth.Token{UID: "verified-uid", Claims: map[string]any{"admin": true}}, user: &auth.UserRecord{CustomClaims: map[string]any{"admin": tc.claim}, Disabled: tc.disabled}}
			uid, allowed, err := (Verifier{Client: client}).VerifyAdmin(context.Background(), "token")
			if err != nil || allowed != tc.want {
				t.Fatalf("allowed=%v err=%v", allowed, err)
			}
			if uid != "verified-uid" {
				t.Fatalf("uid=%q", uid)
			}
			if client.requestedUID != "verified-uid" {
				t.Fatal("must derive UID from verified token")
			}
		})
	}
}
func TestAdminFailsClosedWhenIdentityServiceUnavailable(t *testing.T) {
	for _, tokenFails := range []bool{true, false} {
		client := &fakeAuth{verified: &auth.Token{UID: "uid"}, user: &auth.UserRecord{CustomClaims: map[string]any{"admin": true}}}
		if tokenFails {
			client.tokenErr = errors.New("private upstream failure")
		} else {
			client.userErr = errors.New("private upstream failure")
		}
		uid, allowed, err := (Verifier{Client: client}).VerifyAdmin(context.Background(), "token")
		if allowed || !errors.Is(err, identity.ErrUnavailable) {
			t.Fatal("must fail closed with unavailable")
		}
		if uid != "" {
			t.Fatal("failed identity must not return UID")
		}
	}
}
