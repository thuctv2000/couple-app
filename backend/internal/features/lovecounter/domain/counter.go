// Package domain contains counter rules without Firebase or HTTP dependencies.
package domain

import (
	"crypto/sha256"
	"encoding/json"
	"errors"
	"strings"
	"time"
	"unicode/utf16"
)

var (
	ErrNotFound        = errors.New("counter_not_found")
	ErrInvalid         = errors.New("invalid_counter_operation")
	ErrConflict        = errors.New("counter_version_conflict")
	ErrOperationReused = errors.New("operation_id_reused")
	ErrScope           = errors.New("counter_owner_mismatch")
)

type Payload struct {
	DisplayName string `json:"displayName"`
	PartnerName string `json:"partnerName"`
	StartDate   string `json:"startDate"`
	TimeZoneID  string `json:"timeZoneId"`
}

type Operation struct {
	ID             string  `json:"operationId"`
	BaseVersion    int64   `json:"baseVersion"`
	PayloadVersion int     `json:"payloadVersion"`
	Payload        Payload `json:"payload"`
}

type Counter struct {
	OwnerUID string  `json:"-"`
	Version  int64   `json:"version"`
	Payload  Payload `json:"payload"`
}

// Receipt and Counter must be committed in the same storage transaction.
// Receipts are scoped by the verified UID, never a client-supplied owner.
type Receipt struct {
	OwnerUID    string
	OperationID string
	Fingerprint [32]byte
	Result      Counter
}

type Decision struct {
	Counter Counter
	Receipt Receipt
	Replay  bool
}

// Decide is called inside a repository transaction after reading current state
// and the receipt for this UID/operation. It never persists partial state.
func Decide(uid string, op Operation, current *Counter, prior *Receipt, now time.Time) (Decision, error) {
	if uid == "" || strings.TrimSpace(uid) != uid {
		return Decision{}, ErrScope
	}
	if current != nil && current.OwnerUID != uid {
		return Decision{}, ErrScope
	}
	if !validOperationID(op.ID) || op.BaseVersion < 0 || op.PayloadVersion != 1 {
		return Decision{}, ErrInvalid
	}
	encoded, err := json.Marshal(op)
	if err != nil {
		return Decision{}, ErrInvalid
	}
	fingerprint := sha256.Sum256(encoded)
	if prior != nil {
		if prior.OwnerUID != uid || prior.Result.OwnerUID != uid || prior.OperationID != op.ID {
			return Decision{}, ErrScope
		}
		if prior.Fingerprint != fingerprint {
			return Decision{}, ErrOperationReused
		}
		return Decision{Counter: prior.Result, Receipt: *prior, Replay: true}, nil
	}
	if err := op.Payload.Validate(now); err != nil {
		return Decision{}, err
	}
	version := int64(0)
	if current != nil {
		version = current.Version
	}
	if op.BaseVersion != version {
		return Decision{}, ErrConflict
	}
	if version == int64(^uint64(0)>>1) {
		return Decision{}, ErrInvalid
	}
	result := Counter{OwnerUID: uid, Version: version + 1, Payload: op.Payload}
	receipt := Receipt{OwnerUID: uid, OperationID: op.ID, Fingerprint: fingerprint, Result: result}
	return Decision{Counter: result, Receipt: receipt}, nil
}

func (p Payload) Validate(now time.Time) error {
	for _, name := range []string{p.DisplayName, p.PartnerName} {
		// Match Kotlin String.length; clients trim before creating immutable payloads.
		if strings.TrimSpace(name) != name || len(utf16.Encode([]rune(name))) > 80 {
			return ErrInvalid
		}
	}
	date, err := time.Parse("2006-01-02", p.StartDate)
	if err != nil || date.Format("2006-01-02") != p.StartDate {
		return ErrInvalid
	}
	if p.TimeZoneID == "" || p.TimeZoneID == "Local" {
		return ErrInvalid
	}
	zone, err := time.LoadLocation(p.TimeZoneID)
	if err != nil || p.StartDate > now.In(zone).Format("2006-01-02") {
		return ErrInvalid
	}
	return nil
}

func validOperationID(id string) bool {
	if len(id) != 36 {
		return false
	}
	for i, c := range id {
		if i == 8 || i == 13 || i == 18 || i == 23 {
			if c != '-' {
				return false
			}
		} else if !(c >= '0' && c <= '9' || c >= 'a' && c <= 'f') {
			return false
		}
	}
	return true
}
