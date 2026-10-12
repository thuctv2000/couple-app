package domain

import (
	"errors"
	"time"
)

var (
	ErrNotFound    = errors.New("admin_user_not_found")
	ErrUnavailable = errors.New("admin_metadata_unavailable")
)

type UserMetadata struct {
	UID                   string   `json:"uid"`
	Disabled              bool     `json:"disabled"`
	Anonymous             bool     `json:"anonymous"`
	Providers             []string `json:"providers"`
	CreatedAtMillis       int64    `json:"createdAtMillis"`
	LastLoginAtMillis     int64    `json:"lastLoginAtMillis"`
	LastRefreshAtMillis   int64    `json:"lastRefreshAtMillis"`
	CounterExists         bool     `json:"counterExists"`
	CounterVersion        int64    `json:"counterVersion,omitempty"`
	DeletionRequestStatus string   `json:"deletionRequestStatus"`
}

type AuditEvent struct {
	ActorUID   string
	TargetUID  string
	Action     string
	Outcome    string
	OccurredAt time.Time
}
