package data

import (
	"context"
	"sort"
	"strings"

	"cloud.google.com/go/firestore"
	admindomain "example.com/coupleapp/backend/internal/features/admin/domain"
	"firebase.google.com/go/v4/auth"
	"github.com/google/uuid"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type MetadataRepository struct {
	Auth      *auth.Client
	Firestore *firestore.Client
}

func (r MetadataRepository) Get(ctx context.Context, uid string) (admindomain.UserMetadata, error) {
	user, err := r.Auth.GetUser(ctx, uid)
	if auth.IsUserNotFound(err) {
		return admindomain.UserMetadata{}, admindomain.ErrNotFound
	}
	if err != nil || user.UserMetadata == nil {
		return admindomain.UserMetadata{}, admindomain.ErrUnavailable
	}
	providers := make([]string, 0, len(user.ProviderUserInfo))
	seen := map[string]bool{}
	for _, provider := range user.ProviderUserInfo {
		id := normalizedProvider(provider.ProviderID)
		if !seen[id] {
			seen[id] = true
			providers = append(providers, id)
		}
	}
	sort.Strings(providers)
	metadata := admindomain.UserMetadata{
		UID: uid, Disabled: user.Disabled, Anonymous: len(providers) == 0,
		Providers: providers, CreatedAtMillis: user.UserMetadata.CreationTimestamp,
		LastLoginAtMillis:     user.UserMetadata.LastLogInTimestamp,
		LastRefreshAtMillis:   user.UserMetadata.LastRefreshTimestamp,
		DeletionRequestStatus: "none",
	}
	counter, err := r.Firestore.Collection("users").Doc(uid).Collection("counters").Doc("primary").Get(ctx)
	if err == nil {
		var value struct {
			Version int64 `firestore:"version"`
		}
		if err := counter.DataTo(&value); err != nil || value.Version < 1 {
			return admindomain.UserMetadata{}, admindomain.ErrUnavailable
		}
		metadata.CounterExists = true
		metadata.CounterVersion = value.Version
	} else if status.Code(err) != codes.NotFound {
		return admindomain.UserMetadata{}, admindomain.ErrUnavailable
	}
	deletion, err := r.Firestore.Collection("users").Doc(uid).Collection("deletionRequests").Doc("current").Get(ctx)
	if err == nil {
		value, ok := deletion.Data()["status"].(string)
		if !ok {
			return admindomain.UserMetadata{}, admindomain.ErrUnavailable
		}
		switch value {
		case "requested", "processing", "failed":
			metadata.DeletionRequestStatus = value
		default:
			return admindomain.UserMetadata{}, admindomain.ErrUnavailable
		}
	} else if status.Code(err) != codes.NotFound {
		return admindomain.UserMetadata{}, admindomain.ErrUnavailable
	}
	return metadata, nil
}

func normalizedProvider(value string) string {
	switch strings.ToLower(value) {
	case "google.com", "apple.com", "password", "phone":
		return strings.ToLower(value)
	default:
		return "other"
	}
}

type AuditRepository struct{ Firestore *firestore.Client }

func (r AuditRepository) Record(ctx context.Context, event admindomain.AuditEvent) error {
	if event.ActorUID == "" || event.TargetUID == "" || event.Action != "user_metadata_view" || event.Outcome == "" {
		return admindomain.ErrUnavailable
	}
	_, err := r.Firestore.Collection("adminAudit").Doc(uuid.NewString()).Create(ctx, map[string]any{
		"actorUid": event.ActorUID, "targetUid": event.TargetUID,
		"action": event.Action, "outcome": event.Outcome,
		"occurredAt": event.OccurredAt.UTC(),
	})
	if err != nil {
		return admindomain.ErrUnavailable
	}
	return nil
}
