package httpapi

import (
	"log/slog"
	"net/http"
	"time"

	"github.com/google/uuid"
)

// AccessLog emits an allowlisted request summary. Raw URL, headers, body, IP,
// user IDs and upstream error text must not be added to this event.
func AccessLog(logger *slog.Logger, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		started := time.Now()
		requestID := uuid.NewString() // Never echo an untrusted caller-supplied ID.
		w.Header().Set("X-Request-ID", requestID)
		recorder := &statusWriter{ResponseWriter: w}
		next.ServeHTTP(recorder, r)
		status := recorder.status
		if status == 0 {
			status = http.StatusOK
		}
		route := r.Pattern
		if route == "" {
			route = "unmatched"
		}
		method := r.Method
		switch method {
		case "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "CONNECT", "TRACE":
		default:
			method = "OTHER"
		}
		level := slog.LevelInfo
		if status >= 500 {
			level = slog.LevelError
		} else if status >= 400 {
			level = slog.LevelWarn
		}
		logger.Log(r.Context(), level, "http_request", "request_id", requestID,
			"method", method, "route", route, "status", status, "duration_ms", time.Since(started).Milliseconds())
	})
}

type statusWriter struct {
	http.ResponseWriter
	status int
}

func (w *statusWriter) WriteHeader(status int) {
	if w.status != 0 {
		return
	}
	if status >= 200 {
		w.status = status
	}
	w.ResponseWriter.WriteHeader(status)
}

func (w *statusWriter) Write(body []byte) (int, error) {
	if w.status == 0 {
		w.WriteHeader(http.StatusOK)
	}
	return w.ResponseWriter.Write(body)
}

// Preserve ResponseController support for underlying deadlines and flushing.
func (w *statusWriter) Unwrap() http.ResponseWriter { return w.ResponseWriter }
