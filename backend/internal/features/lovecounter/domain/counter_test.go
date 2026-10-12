package domain

import (
	"errors"
	"strings"
	"testing"
	"time"
)

func operation() Operation {
	return Operation{ID: "91cdbf3b-460f-4230-97d8-cd6df142f399", PayloadVersion: 1,
		Payload: Payload{DisplayName: "Linh", PartnerName: "Nam", StartDate: "2026-10-09", TimeZoneID: "Asia/Ho_Chi_Minh"}}
}

var now = time.Date(2026, 10, 9, 0, 0, 0, 0, time.UTC)

func TestCreateRetryAndNextRevision(t *testing.T) {
	op := operation()
	first, err := Decide("user-a", op, nil, nil, now)
	if err != nil || first.Counter.Version != 1 || first.Replay {
		t.Fatalf("first = %+v, %v", first, err)
	}
	next := op
	next.ID = "a1cdbf3b-460f-4230-97d8-cd6df142f399"
	next.BaseVersion = 1
	next.Payload.DisplayName = "Updated"
	second, err := Decide("user-a", next, &first.Counter, nil, now)
	if err != nil || second.Counter.Version != 2 {
		t.Fatalf("second = %+v, %v", second, err)
	}
	// A delayed retry returns its original ACK, not the newer counter/version.
	replay, err := Decide("user-a", op, &second.Counter, &first.Receipt, now)
	if err != nil || !replay.Replay || replay.Counter != first.Counter {
		t.Fatalf("replay = %+v, %v", replay, err)
	}
}

func TestConflictsDoNotOverwrite(t *testing.T) {
	op := operation()
	first, _ := Decide("user-a", op, nil, nil, now)
	other := op
	other.ID = "a1cdbf3b-460f-4230-97d8-cd6df142f399"
	other.Payload.DisplayName = "Other device"
	if _, err := Decide("user-a", other, &first.Counter, nil, now); !errors.Is(err, ErrConflict) {
		t.Fatal(err)
	}
	if first.Counter.Payload.DisplayName != "Linh" {
		t.Fatal("current was mutated")
	}
	op.Payload.DisplayName = "Changed payload with old operation ID"
	if _, err := Decide("user-a", op, &first.Counter, &first.Receipt, now); !errors.Is(err, ErrOperationReused) {
		t.Fatal(err)
	}
}

func TestOwnerScopeCannotCrossAccounts(t *testing.T) {
	op := operation()
	first, _ := Decide("user-a", op, nil, nil, now)
	for _, tc := range []struct {
		current *Counter
		prior   *Receipt
	}{{&first.Counter, nil}, {nil, &first.Receipt}} {
		if _, err := Decide("user-b", op, tc.current, tc.prior, now); !errors.Is(err, ErrScope) {
			t.Fatal(err)
		}
	}
	if _, err := Decide("", op, nil, nil, now); !errors.Is(err, ErrScope) {
		t.Fatal(err)
	}
}

func TestInvalidPayloadAndProtocol(t *testing.T) {
	cases := map[string]func(*Operation){
		"invalid date":     func(o *Operation) { o.Payload.StartDate = "2026-02-30" },
		"future date":      func(o *Operation) { o.Payload.StartDate = "2026-10-10" },
		"unknown zone":     func(o *Operation) { o.Payload.TimeZoneID = "invalid" },
		"host zone":        func(o *Operation) { o.Payload.TimeZoneID = "Local" },
		"long name":        func(o *Operation) { o.Payload.DisplayName = strings.Repeat("a", 81) },
		"UTF16 name":       func(o *Operation) { o.Payload.DisplayName = strings.Repeat("💗", 41) },
		"untrimmed name":   func(o *Operation) { o.Payload.DisplayName = " Linh" },
		"invalid ID":       func(o *Operation) { o.ID = "../receipt" },
		"negative version": func(o *Operation) { o.BaseVersion = -1 },
		"unknown payload":  func(o *Operation) { o.PayloadVersion = 2 },
	}
	for name, change := range cases {
		t.Run(name, func(t *testing.T) {
			op := operation()
			change(&op)
			if _, err := Decide("user-a", op, nil, nil, now); !errors.Is(err, ErrInvalid) {
				t.Fatal(err)
			}
		})
	}
}

func TestDateUsesCounterZoneAtMidnight(t *testing.T) {
	op := operation()
	// October 9 in Vietnam, October 8 in UTC and Los Angeles.
	instant := time.Date(2026, 10, 8, 18, 0, 0, 0, time.UTC)
	if _, err := Decide("user-a", op, nil, nil, instant); err != nil {
		t.Fatal(err)
	}
	op.Payload.TimeZoneID = "America/Los_Angeles"
	if _, err := Decide("user-a", op, nil, nil, instant); !errors.Is(err, ErrInvalid) {
		t.Fatal(err)
	}
}
