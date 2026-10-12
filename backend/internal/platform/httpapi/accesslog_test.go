package httpapi

import (
	"bytes"
	"encoding/json"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/google/uuid"
)

func TestAccessLogExcludesPrivateRequestData(t *testing.T) {
	var output bytes.Buffer
	router := http.NewServeMux()
	router.HandleFunc("PUT /v1/users/{uid}", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusConflict)
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"private":"response-secret"}`))
	})
	handler := AccessLog(slog.New(slog.NewJSONHandler(&output, nil)), router)
	request := httptest.NewRequest("PUT", "/v1/users/private-user?token=query-secret", strings.NewReader("body-secret"))
	request.Header.Set("Authorization", "Bearer header-secret")
	request.Header.Set("X-Request-ID", "caller-private-id")
	request.Header.Set("User-Agent", "private-user-agent")
	response := httptest.NewRecorder()
	handler.ServeHTTP(response, request)
	if response.Code != 409 {
		t.Fatalf("status=%d", response.Code)
	}
	var event map[string]any
	if err := json.Unmarshal(output.Bytes(), &event); err != nil {
		t.Fatal(err)
	}
	if event["route"] != "PUT /v1/users/{uid}" || event["status"] != float64(409) || event["level"] != "WARN" {
		t.Fatalf("bad summary: %v", event)
	}
	id := response.Header().Get("X-Request-ID")
	if _, err := uuid.Parse(id); err != nil {
		t.Fatal(err)
	}
	if event["request_id"] != id {
		t.Fatal("response and log must share request ID")
	}
	for _, secret := range []string{"private-user", "query-secret", "body-secret", "header-secret", "caller-private-id", "private-user-agent", "response-secret", request.RemoteAddr} {
		if strings.Contains(output.String(), secret) {
			t.Fatalf("private value leaked: %q", secret)
		}
	}
	for key := range event {
		switch key {
		case "time", "level", "msg", "request_id", "method", "route", "status", "duration_ms":
		default:
			t.Fatalf("unexpected field %s", key)
		}
	}
}

func TestAccessLogStatusAndUnknownRoutes(t *testing.T) {
	for _, scenario := range []struct {
		name, method, path, level, route string
		status                           int
	}{
		{"implicit", "GET", "/ok", "INFO", "GET /ok", 200},
		{"unavailable", "GET", "/unavailable", "ERROR", "GET /unavailable", 503},
		{"missing", "GET", "/private-path-secret", "WARN", "unmatched", 404},
		{"unknown-method", "PRIVATESECRET", "/private-path-secret", "WARN", "unmatched", 404},
	} {
		t.Run(scenario.name, func(t *testing.T) {
			var output bytes.Buffer
			router := http.NewServeMux()
			router.HandleFunc("GET /ok", func(w http.ResponseWriter, r *http.Request) { _, _ = w.Write([]byte("ok")) })
			router.HandleFunc("GET /unavailable", func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(503) })
			handler := AccessLog(slog.New(slog.NewJSONHandler(&output, nil)), router)
			response := httptest.NewRecorder()
			handler.ServeHTTP(response, httptest.NewRequest(scenario.method, scenario.path, nil))
			var event map[string]any
			if err := json.Unmarshal(output.Bytes(), &event); err != nil {
				t.Fatal(err)
			}
			if event["status"] != float64(scenario.status) || event["level"] != scenario.level || event["route"] != scenario.route {
				t.Fatalf("bad event: %v", event)
			}
			if strings.Contains(output.String(), "secret") || strings.Contains(output.String(), "PRIVATESECRET") {
				t.Fatal("raw path or method leaked")
			}
		})
	}
}
