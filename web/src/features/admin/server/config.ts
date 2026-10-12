export type AdminConfig = { apiOrigin: string; webOrigin: string; cookieName: string; secure: boolean };

export function readAdminConfig(env: Record<string, string | undefined>): AdminConfig | null {
  try {
    if (!env.ADMIN_API_ORIGIN || !env.ADMIN_WEB_ORIGIN) return null;
    const api = new URL(env.ADMIN_API_ORIGIN);
    const web = new URL(env.ADMIN_WEB_ORIGIN);
    const local = env.APP_ENV === "local";
    for (const url of [api, web]) {
      if (url.username || url.password || url.pathname !== "/" || url.search || url.hash) return null;
      if (url.protocol !== "https:" && !(local && url.protocol === "http:" && ["127.0.0.1", "localhost", "[::1]"].includes(url.hostname))) return null;
    }
    return { apiOrigin: api.origin, webOrigin: web.origin, secure: web.protocol === "https:", cookieName: web.protocol === "https:" ? "__Host-couple_admin" : "couple_admin_local" };
  } catch { return null; }
}

export function validMutationOrigin(request: Request, config: AdminConfig): boolean {
  return request.headers.get("origin") === config.webOrigin && request.headers.get("sec-fetch-site") !== "cross-site";
}

export type AdminLoginConfig = { apiKey: string; authDomain: string; projectId: string; appId: string; emulatorUrl?: string };
export function readAdminLoginConfig(env: Record<string, string | undefined>): AdminLoginConfig | null {
  const apiKey = env.NEXT_PUBLIC_FIREBASE_API_KEY;
  const authDomain = env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN;
  const projectId = env.NEXT_PUBLIC_FIREBASE_PROJECT_ID;
  const appId = env.NEXT_PUBLIC_FIREBASE_APP_ID;
  if (!apiKey || !authDomain || !projectId || !appId) return null;
  const emulatorUrl = env.NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_URL;
  if (emulatorUrl) {
    try {
      const url = new URL(emulatorUrl);
      if (env.APP_ENV !== "local" || !projectId.startsWith("demo-") || url.protocol !== "http:" || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname) || url.username || url.password || url.pathname !== "/" || url.search || url.hash) return null;
    } catch { return null; }
  } else if (projectId.startsWith("demo-")) return null;
  return { apiKey, authDomain, projectId, appId, ...(emulatorUrl ? { emulatorUrl } : {}) };
}
