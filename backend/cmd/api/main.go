package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"
	_ "time/tzdata" // Keep IANA zones available in minimal deployment images.

	admindata "example.com/coupleapp/backend/internal/features/admin/data"
	admintransport "example.com/coupleapp/backend/internal/features/admin/transport"
	"example.com/coupleapp/backend/internal/features/lovecounter/data"
	"example.com/coupleapp/backend/internal/features/lovecounter/transport"
	"example.com/coupleapp/backend/internal/platform/firebaseauth"
	"example.com/coupleapp/backend/internal/platform/httpapi"
	firebase "firebase.google.com/go/v4"
)

func main() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	var counter http.Handler = transport.Handler{}
	var admin http.Handler = admintransport.SessionHandler{}
	var adminMetadata http.Handler = admintransport.MetadataHandler{}
	projectID := os.Getenv("FIREBASE_PROJECT_ID")
	usingEmulators := os.Getenv("FIREBASE_AUTH_EMULATOR_HOST") != "" || os.Getenv("FIRESTORE_EMULATOR_HOST") != ""
	if usingEmulators && (os.Getenv("APP_ENV") != "local" || !strings.HasPrefix(projectID, "demo-") || os.Getenv("FIREBASE_AUTH_EMULATOR_HOST") == "" || os.Getenv("FIRESTORE_EMULATOR_HOST") == "") {
		logger.Error("emulators_require_local_demo_project_and_both_hosts")
		os.Exit(1)
	}
	if projectID != "" {
		ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
		defer cancel()
		app, err := firebase.NewApp(ctx, &firebase.Config{ProjectID: projectID})
		if err != nil {
			logger.Error("firebase_initialization_failed")
			os.Exit(1)
		}
		authClient, err := app.Auth(ctx)
		if err != nil {
			logger.Error("firebase_auth_initialization_failed")
			os.Exit(1)
		}
		store, err := app.Firestore(ctx)
		if err != nil {
			logger.Error("firestore_initialization_failed")
			os.Exit(1)
		}
		defer store.Close()
		counter = transport.Handler{Auth: firebaseauth.Verifier{Client: authClient}, Store: data.FirestoreRepository{Client: store}}
		adminVerifier := firebaseauth.Verifier{Client: authClient}
		admin = admintransport.SessionHandler{Auth: adminVerifier}
		adminMetadata = admintransport.MetadataHandler{
			Auth:  adminVerifier,
			Store: admindata.MetadataRepository{Auth: authClient, Firestore: store},
			Audit: admindata.AuditRepository{Firestore: store},
		}
	} else {
		logger.Info("counter_cloud_disabled", "reason", "missing_firebase_project_id")
	}
	addr := os.Getenv("HTTP_ADDR")
	if addr == "" {
		addr = "127.0.0.1:8080"
	}
	server := &http.Server{
		Addr: addr, Handler: httpapi.AccessLog(logger, httpapi.NewRouter(counter, admin, adminMetadata)),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       15 * time.Second, WriteTimeout: 15 * time.Second,
		IdleTimeout: 60 * time.Second,
	}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	errCh := make(chan error, 1)
	go func() { errCh <- server.ListenAndServe() }()
	logger.Info("api_started", "address", addr)
	select {
	case err := <-errCh:
		if !errors.Is(err, http.ErrServerClosed) {
			logger.Error("api_failed", "error", err.Error())
			os.Exit(1)
		}
	case <-ctx.Done():
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		if err := server.Shutdown(shutdownCtx); err != nil {
			logger.Error("shutdown_failed", "error", err.Error())
			os.Exit(1)
		}
	}
}
