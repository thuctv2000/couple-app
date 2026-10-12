import { NextResponse } from "next/server";
import { readAdminConfig, validMutationOrigin } from "@/features/admin/server/config";
import { verifyAdmin } from "@/features/admin/server/verify";

export async function POST(request: Request) {
  const config = readAdminConfig(process.env);
  const respond = (error: string, status: number) => NextResponse.json({ error }, { status, headers: { "Cache-Control": "no-store" } });
  if (!config) return respond("not_configured", 503);
  if (!validMutationOrigin(request, config)) return respond("invalid_origin", 403);
  if (request.headers.get("content-type")?.split(";")[0] !== "application/json") return respond("invalid_request", 400);
  // Bound streamed bytes too; Content-Length is untrusted and may be absent.
  const reader = request.body?.getReader();
  if (!reader) return respond("invalid_request", 400);
  const chunks: Uint8Array[] = []; let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      size += value.byteLength;
      if (size > 4096) { await reader.cancel(); return respond("request_too_large", 413); }
      chunks.push(value);
    }
    const bytes = new Uint8Array(size); let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
    const body: unknown = JSON.parse(new TextDecoder().decode(bytes));
    if (!body || typeof body !== "object" || !("idToken" in body) || typeof body.idToken !== "string" || Object.keys(body).length !== 1) return respond("invalid_request", 400);
    const result = await verifyAdmin(config, body.idToken);
    if (result !== "allowed") return respond(result, result === "unauthenticated" ? 401 : result === "forbidden" ? 403 : 503);
    const response = NextResponse.json({ authenticated: true }, { headers: { "Cache-Control": "no-store" } });
    response.cookies.set(config.cookieName, body.idToken, { httpOnly: true, secure: config.secure, sameSite: "strict", path: "/", maxAge: 3300 });
    return response;
  } catch { return respond("invalid_request", 400); }
}
