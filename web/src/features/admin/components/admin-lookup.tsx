"use client";

import { useState } from "react";
import type { FormEvent } from "react";

type Metadata = {
  uid: string;
  disabled: boolean;
  anonymous: boolean;
  providers: string[];
  createdAtMillis: number;
  lastLoginAtMillis: number;
  lastRefreshAtMillis: number;
  counterExists: boolean;
  counterVersion?: number;
  deletionRequestStatus: string;
};

function date(value: number) {
  return value > 0 ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "Chưa có";
}

export function AdminLookup() {
  const [uid, setUid] = useState("");
  const [result, setResult] = useState<Metadata | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const response = await fetch("/api/admin/lookup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ uid: uid.trim() }),
        credentials: "same-origin",
        signal: AbortSignal.timeout(12000),
      });
      if (response.ok) {
        setResult(await response.json() as Metadata);
        return;
      }
      setError(response.status === 404
        ? "Không tìm thấy tài khoản."
        : response.status === 403
          ? "Phiên hiện tại không có quyền quản trị."
          : response.status === 401
            ? "Phiên đã hết hạn. Hãy đăng nhập lại."
            : response.status === 400
              ? "UID chưa hợp lệ."
              : "Chưa tra cứu được. Hãy thử lại sau.");
    } catch {
      setError("Chưa kết nối được dịch vụ. Hãy thử lại sau.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="admin-tool" aria-labelledby="lookup-title">
      <h2 id="lookup-title">Tra cứu metadata tài khoản</h2>
      <p>Nhập chính xác Firebase UID. Mỗi lần tra cứu được ghi audit; nội dung bộ đếm và thông tin liên hệ không hiển thị.</p>
      <form onSubmit={submit}>
        <label htmlFor="account-uid">Firebase UID</label>
        <div className="admin-lookup-row">
          <input id="account-uid" value={uid} onChange={(event) => setUid(event.target.value)} maxLength={128} autoComplete="off" spellCheck={false} required />
          <button className="button primary" disabled={busy}>{busy ? "Đang tra cứu…" : "Tra cứu"}</button>
        </div>
      </form>
      {error && <p role="alert" className="admin-error">{error}</p>}
      {result && (
        <dl className="metadata-grid">
          <div><dt>UID</dt><dd>{result.uid}</dd></div>
          <div><dt>Trạng thái</dt><dd>{result.disabled ? "Đã vô hiệu hóa" : "Đang hoạt động"}</dd></div>
          <div><dt>Loại tài khoản</dt><dd>{result.anonymous ? "Anonymous" : result.providers.join(", ") || "Khác"}</dd></div>
          <div><dt>Tạo lúc</dt><dd>{date(result.createdAtMillis)}</dd></div>
          <div><dt>Đăng nhập gần nhất</dt><dd>{date(result.lastLoginAtMillis)}</dd></div>
          <div><dt>Hoạt động gần nhất</dt><dd>{date(result.lastRefreshAtMillis)}</dd></div>
          <div><dt>Đồng bộ bộ đếm</dt><dd>{result.counterExists ? `Version ${result.counterVersion}` : "Chưa có"}</dd></div>
          <div><dt>Yêu cầu xóa</dt><dd>{result.deletionRequestStatus === "none" ? "Không có" : result.deletionRequestStatus}</dd></div>
        </dl>
      )}
    </section>
  );
}
