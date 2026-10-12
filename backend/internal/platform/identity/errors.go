package identity

import "errors"

// ErrUnavailable means verification could not finish, not that login is invalid.
var ErrUnavailable = errors.New("identity_service_unavailable")
