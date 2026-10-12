import type { AdminConfig } from "./config";
export type AdminVerification = "allowed" | "unauthenticated" | "forbidden" | "unavailable";

export async function verifyAdmin(config: AdminConfig, token: string | undefined, fetcher: typeof fetch = fetch): Promise<AdminVerification> {
  if (!token || token.length > 3800 || !/^[A-Za-z0-9_.-]+$/.test(token)) return "unauthenticated";
  try {
    const response = await fetcher(`${config.apiOrigin}/v1/admin/session`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store", redirect: "error", signal: AbortSignal.timeout(8000),
    });
    if (response.status === 401) return "unauthenticated";
    if (response.status === 403) return "forbidden";
    if (response.status !== 200) return "unavailable";
    const body: unknown = await response.json();
    return body !== null && typeof body === "object" && "role" in body && body.role === "admin" ? "allowed" : "unavailable";
  } catch { return "unavailable"; }
}
