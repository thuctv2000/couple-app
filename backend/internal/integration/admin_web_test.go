package integration

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"net"
	"net/http"
	"net/url"
	"os"
	"strings"
	"testing"
	"time"
)

func TestNextAdminSessionAgainstFirebaseEmulator(t *testing.T) {
	origin := os.Getenv("ADMIN_WEB_TEST_ORIGIN")
	if origin == "" {
		t.Skip("local Next.js admin instance required")
	}
	parsed, err := url.Parse(origin)
	if err != nil || parsed.Scheme != "http" || parsed.User != nil || parsed.RawQuery != "" || parsed.Fragment != "" || parsed.Path != "" || net.ParseIP(parsed.Hostname()) == nil || !net.ParseIP(parsed.Hostname()).IsLoopback() {
		t.Fatal("loopback web origin required")
	}
	a, db := emulator(t)
	uid, token := guest(t, a, db)
	client := &http.Client{Timeout: 15 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	call := func(method, path, requestOrigin string, body []byte, cookie *http.Cookie) (*http.Response, []byte) {
		t.Helper()
		r, err := http.NewRequest(method, origin+path, bytes.NewReader(body))
		if err != nil {
			t.Fatal(err)
		}
		if requestOrigin != "" {
			r.Header.Set("Origin", requestOrigin)
		}
		if body != nil {
			r.Header.Set("Content-Type", "application/json")
		}
		if cookie != nil {
			r.AddCookie(cookie)
		}
		response, err := client.Do(r)
		if err != nil {
			t.Fatal("local web request failed")
		}
		defer response.Body.Close()
		payload, err := io.ReadAll(response.Body)
		if err != nil {
			t.Fatal(err)
		}
		return response, payload
	}
	body, _ := json.Marshal(map[string]string{"idToken": token})
	if r, _ := call("POST", "/api/admin/session", "", body, nil); r.StatusCode != 403 {
		t.Fatalf("missing origin status=%d", r.StatusCode)
	}
	if r, _ := call("POST", "/api/admin/session", origin, []byte(strings.Repeat("x", 4097)), nil); r.StatusCode != 413 {
		t.Fatalf("oversized body status=%d", r.StatusCode)
	}
	if r, _ := call("POST", "/api/admin/session", origin, []byte(`{"idToken":"token","admin":true}`), nil); r.StatusCode != 400 {
		t.Fatalf("extra field status=%d", r.StatusCode)
	}

	if r, _ := call("GET", "/admin", "", nil, nil); r.StatusCode != 307 {
		t.Fatalf("unauthenticated admin status=%d", r.StatusCode)
	}
	if r, _ := call("POST", "/api/admin/session", "http://attacker.invalid", body, nil); r.StatusCode != 403 {
		t.Fatalf("cross-origin status=%d", r.StatusCode)
	}
	if r, _ := call("POST", "/api/admin/session", origin, body, nil); r.StatusCode != 403 {
		t.Fatalf("ordinary user status=%d", r.StatusCode)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	if err := a.SetCustomUserClaims(ctx, uid, map[string]any{"admin": true}); err != nil {
		t.Fatal(err)
	}
	login, _ := call("POST", "/api/admin/session", origin, body, nil)
	if login.StatusCode != 200 {
		t.Fatalf("admin login status=%d", login.StatusCode)
	}
	cookies := login.Cookies()
	if len(cookies) != 1 {
		t.Fatal("expected one session cookie")
	}
	cookie := cookies[0]
	if !cookie.HttpOnly || cookie.SameSite != http.SameSiteStrictMode || cookie.Path != "/" || cookie.MaxAge != 3300 {
		t.Fatal("incorrect cookie protections")
	}
	page, html := call("GET", "/admin", "", nil, cookie)
	if page.StatusCode != 200 || !strings.Contains(string(html), "đã xác minh") {
		t.Fatal("admin page not authorized")
	}
	if strings.Contains(string(html), token) {
		t.Fatal("token leaked into HTML")
	}
	lookupBody, _ := json.Marshal(map[string]string{"uid": uid})
	if r, _ := call("POST", "/api/admin/lookup", origin, []byte(strings.Repeat("x", 1025)), cookie); r.StatusCode != 413 {
		t.Fatalf("oversized lookup body status=%d", r.StatusCode)
	}
	if r, _ := call("POST", "/api/admin/lookup", "http://attacker.invalid", lookupBody, cookie); r.StatusCode != 403 {
		t.Fatalf("cross-origin lookup status=%d", r.StatusCode)
	}
	lookup, lookupPayload := call("POST", "/api/admin/lookup", origin, lookupBody, cookie)
	if lookup.StatusCode != 200 || !strings.Contains(string(lookupPayload), `"uid":"`+uid+`"`) {
		t.Fatalf("metadata lookup status=%d body=%s", lookup.StatusCode, lookupPayload)
	}
	audits, err := db.Collection("adminAudit").Where("actorUid", "==", uid).Documents(ctx).GetAll()
	if err != nil {
		t.Fatal(err)
	}
	if len(audits) != 1 || audits[0].Data()["targetUid"] != uid || audits[0].Data()["outcome"] != "success" {
		t.Fatalf("metadata audit missing or invalid: count=%d", len(audits))
	}
	t.Cleanup(func() {
		cleanupCtx, cleanupCancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cleanupCancel()
		for _, audit := range audits {
			_, _ = audit.Ref.Delete(cleanupCtx)
		}
	})
	if err := a.SetCustomUserClaims(ctx, uid, map[string]any{"admin": false}); err != nil {
		t.Fatal(err)
	}
	if r, _ := call("POST", "/api/admin/lookup", origin, lookupBody, cookie); r.StatusCode != 403 {
		t.Fatalf("revoked admin lookup status=%d", r.StatusCode)
	}
	_, html = call("GET", "/admin", "", nil, cookie)
	if !strings.Contains(string(html), "Không có quyền quản trị") || strings.Contains(string(html), "Phiên quản trị") {
		t.Fatal("removed role retained protected content")
	}
	logout, _ := call("POST", "/api/admin/logout", origin, nil, cookie)
	if logout.StatusCode != 303 || len(logout.Cookies()) != 1 || logout.Cookies()[0].MaxAge != -1 {
		t.Fatal("logout did not clear cookie")
	}
}
