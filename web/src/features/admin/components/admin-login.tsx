"use client";

import { useState } from "react";
import { getApps, initializeApp } from "firebase/app";
import { type Auth, connectAuthEmulator, getAuth, GoogleAuthProvider, inMemoryPersistence, setPersistence, signInWithPopup, signOut } from "firebase/auth";
import type { AdminLoginConfig } from "../server/config";

export function AdminLogin({ config }: { config: AdminLoginConfig }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function login() {
    if (busy) return;
    setBusy(true); setError(null);
    let auth: Auth | undefined;
    try {
      const name = "couple-admin";
      const existing = getApps().find(app => app.name === name);
      const app = existing ?? initializeApp(config, name);
      auth = getAuth(app);
      if (!existing && config.emulatorUrl) connectAuthEmulator(auth, config.emulatorUrl);
      await setPersistence(auth, inMemoryPersistence);
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: "select_account" });
      const result = await signInWithPopup(auth, provider);
      const idToken = await result.user.getIdToken(true);
      const response = await fetch("/api/admin/session", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken }), credentials: "same-origin", signal: AbortSignal.timeout(12000),
      });
      if (response.ok) { window.location.assign("/admin"); return; }
      setError(response.status === 403 ? "Tài khoản này chưa có quyền quản trị." : response.status === 401 ? "Phiên đăng nhập chưa hợp lệ. Hãy thử lại." : "Chưa kết nối được dịch vụ xác thực. Hãy thử lại sau.");
    } catch (cause: unknown) {
      const code = cause && typeof cause === "object" && "code" in cause ? cause.code : "";
      setError(code === "auth/popup-closed-by-user" || code === "auth/cancelled-popup-request" ? "Bạn đã hủy đăng nhập." : code === "auth/popup-blocked" ? "Trình duyệt đang chặn cửa sổ đăng nhập. Hãy cho phép cửa sổ này và thử lại." : "Chưa đăng nhập được. Hãy kiểm tra kết nối và thử lại.");
    } finally {
      if (auth) await signOut(auth).catch(() => {});
      setBusy(false);
    }
  }
  return <div className="admin-login"><button className="button primary" onClick={login} disabled={busy}>{busy ? "Đang xác minh…" : "Đăng nhập bằng Google"}</button>{error && <p role="alert">{error}</p>}<p className="muted">Chỉ tài khoản được cấp quyền mới truy cập được. Đăng nhập không tự cấp quyền quản trị.</p></div>;
}
