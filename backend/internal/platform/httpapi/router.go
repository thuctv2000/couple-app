package httpapi

import "net/http"

func NewRouter(counter, adminSession, adminMetadata http.Handler) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Cache-Control", "no-store")
		_, _ = w.Write([]byte(`{"status":"ok"}`))
	})
	if counter != nil {
		mux.Handle("/v1/me/counter", counter)
	}
	if adminSession != nil {
		mux.Handle("/v1/admin/session", adminSession)
	}
	if adminMetadata != nil {
		mux.Handle("/v1/admin/users/{uid}/metadata", adminMetadata)
	}
	return mux
}
