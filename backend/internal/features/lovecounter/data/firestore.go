package data

import (
	"context"
	"encoding/hex"
	"errors"
	"strings"
	"time"

	"cloud.google.com/go/firestore"
	"example.com/coupleapp/backend/internal/features/lovecounter/domain"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type FirestoreRepository struct{ Client *firestore.Client }

type counterDocument struct {
	Version int64          `firestore:"version"`
	Payload domain.Payload `firestore:"payload"`
}
type receiptDocument struct {
	Fingerprint string          `firestore:"fingerprint"`
	Result      counterDocument `firestore:"result"`
}

func validUID(uid string) bool {
	return uid != "" && !strings.Contains(uid, "/") && uid != "." && uid != ".."
}

func (r FirestoreRepository) Get(ctx context.Context, uid string) (domain.Counter, error) {
	if !validUID(uid) {
		return domain.Counter{}, domain.ErrScope
	}
	snapshot, err := r.Client.Collection("users").Doc(uid).Collection("counters").Doc("primary").Get(ctx)
	if status.Code(err) == codes.NotFound {
		return domain.Counter{}, domain.ErrNotFound
	}
	if err != nil {
		return domain.Counter{}, err
	}
	var doc counterDocument
	if err := snapshot.DataTo(&doc); err != nil {
		return domain.Counter{}, err
	}
	return domain.Counter{OwnerUID: uid, Version: doc.Version, Payload: doc.Payload}, nil
}

func (r FirestoreRepository) Apply(ctx context.Context, uid string, op domain.Operation) (domain.Decision, error) {
	if !validUID(uid) {
		return domain.Decision{}, domain.ErrScope
	}
	// Validate the path component before constructing any Firestore reference.
	if len(op.ID) != 36 || strings.Contains(op.ID, "/") {
		return domain.Decision{}, domain.ErrInvalid
	}
	owner := r.Client.Collection("users").Doc(uid)
	counterRef := owner.Collection("counters").Doc("primary")
	receiptRef := owner.Collection("counterOperations").Doc(op.ID)
	var result domain.Decision
	err := r.Client.RunTransaction(ctx, func(ctx context.Context, tx *firestore.Transaction) error {
		result = domain.Decision{}
		var current *domain.Counter
		var prior *domain.Receipt
		snapshot, err := tx.Get(counterRef)
		if err == nil {
			var doc counterDocument
			if err := snapshot.DataTo(&doc); err != nil {
				return err
			}
			current = &domain.Counter{OwnerUID: uid, Version: doc.Version, Payload: doc.Payload}
		} else if status.Code(err) != codes.NotFound {
			return err
		}
		snapshot, err = tx.Get(receiptRef)
		if err == nil {
			var doc receiptDocument
			if err := snapshot.DataTo(&doc); err != nil {
				return err
			}
			bytes, err := hex.DecodeString(doc.Fingerprint)
			if err != nil || len(bytes) != 32 {
				return errors.New("invalid_stored_receipt")
			}
			prior = &domain.Receipt{OwnerUID: uid, OperationID: op.ID, Result: domain.Counter{OwnerUID: uid, Version: doc.Result.Version, Payload: doc.Result.Payload}}
			copy(prior.Fingerprint[:], bytes)
		} else if status.Code(err) != codes.NotFound {
			return err
		}
		decision, err := domain.Decide(uid, op, current, prior, time.Now())
		if err != nil {
			return err
		}
		result = decision
		if decision.Replay {
			return nil
		}
		doc := counterDocument{Version: decision.Counter.Version, Payload: decision.Counter.Payload}
		if err := tx.Set(counterRef, doc); err != nil {
			return err
		}
		return tx.Create(receiptRef, receiptDocument{Fingerprint: hex.EncodeToString(decision.Receipt.Fingerprint[:]), Result: doc})
	})
	if err != nil {
		return domain.Decision{}, err
	}
	return result, nil
}
