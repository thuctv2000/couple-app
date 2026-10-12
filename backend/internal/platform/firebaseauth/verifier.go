package firebaseauth

import (
	"context"
	"example.com/coupleapp/backend/internal/platform/identity"
	"firebase.google.com/go/v4/auth"
)

type AuthClient interface {
	VerifyIDTokenAndCheckRevoked(context.Context, string) (*auth.Token, error)
	GetUser(context.Context, string) (*auth.UserRecord, error)
}

type Verifier struct{ Client AuthClient }

func (v Verifier) Verify(ctx context.Context, token string) (string, error) {
	verified, err := v.Client.VerifyIDTokenAndCheckRevoked(ctx, token)
	if err != nil {
		if auth.IsIDTokenInvalid(err) || auth.IsIDTokenExpired(err) || auth.IsIDTokenRevoked(err) || auth.IsUserDisabled(err) || auth.IsUserNotFound(err) || auth.IsTenantIDMismatch(err) {
			return "", err
		}
		return "", identity.ErrUnavailable
	}
	return verified.UID, nil
}

// Re-read the current server-side role so removing admin does not wait for an
// already-issued token to expire. No endpoint in this API grants this claim.
func (v Verifier) VerifyAdmin(ctx context.Context, token string) (string, bool, error) {
	uid, err := v.Verify(ctx, token)
	if err != nil {
		return "", false, err
	}
	user, err := v.Client.GetUser(ctx, uid)
	if err != nil {
		if auth.IsUserNotFound(err) {
			return "", false, err
		}
		return "", false, identity.ErrUnavailable
	}
	admin, _ := user.CustomClaims["admin"].(bool)
	return uid, admin && !user.Disabled, nil
}
