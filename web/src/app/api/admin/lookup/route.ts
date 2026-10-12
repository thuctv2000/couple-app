import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { readAdminConfig, validMutationOrigin } from "@/features/admin/server/config";

export async function POST(request: Request) {
  const config = readAdminConfig(process.env);
  const respond = (error: string, status: number) =>
    NextResponse.json({ error }, { status, headers: { "Cache-Control": "no-store" } });
  if (!config) return respond("not_configured", 503);
  if (!validMutationOrigin(request, config)) return respond("invalid_origin", 403);
  if (request.headers.get("content-type")?.split(";")[0] !== "application/json") {
    return respond("invalid_request", 400);
  }
  let body: unknown;
  try {
    body = JSON.parse(await readBoundedBody(request, 1024));
  } catch (error) {
    return respond(error instanceof BodyTooLargeError ? "request_too_large" : "invalid_request", error instanceof BodyTooLargeError ? 413 : 400);
  }
  if (!body || typeof body !== "object" || !("uid" in body) || typeof body.uid !== "string" || Object.keys(body).length !== 1) {
    return respond("invalid_request", 400);
  }
  const uid = body.uid.trim();
  if (!uid || uid.length > 128 || uid === "." || uid === ".." || /[\\/]/.test(uid)) {
    return respond("invalid_uid", 400);
  }
  const token = (await cookies()).get(config.cookieName)?.value;
  if (!token) return respond("unauthenticated", 401);
  try {
    const upstream = await fetch(`${config.apiOrigin}/v1/admin/users/${encodeURIComponent(uid)}/metadata`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      redirect: "error",
      signal: AbortSignal.timeout(8000),
    });
    if (!upstream.ok) {
      const knownStatus = [401, 403, 404].includes(upstream.status);
      const code = upstream.status === 401 ? "unauthenticated" : upstream.status === 403 ? "forbidden" : upstream.status === 404 ? "user_not_found" : "unavailable";
      return respond(code, knownStatus ? upstream.status : 503);
    }
    const result: unknown = await upstream.json();
    if (!validMetadata(result, uid)) return respond("unavailable", 503);
    return NextResponse.json(result, { headers: { "Cache-Control": "no-store" } });
  } catch {
    return respond("unavailable", 503);
  }
}

class BodyTooLargeError extends Error {}

async function readBoundedBody(request: Request, maximumBytes: number): Promise<string> {
  if (!request.body) return "";
  const reader = request.body.getReader();
  const decoder = new TextDecoder();
  let bytes = 0;
  let result = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    bytes += value.byteLength;
    if (bytes > maximumBytes) {
      await reader.cancel();
      throw new BodyTooLargeError();
    }
    result += decoder.decode(value, { stream: true });
  }
  return result + decoder.decode();
}

function validMetadata(value: unknown, uid: string): value is Record<string, unknown> {
  if (!value || typeof value !== "object") return false;
  const v = value as Record<string, unknown>;
  return v.uid === uid &&
    typeof v.disabled === "boolean" &&
    typeof v.anonymous === "boolean" &&
    Array.isArray(v.providers) &&
    v.providers.every((provider) => typeof provider === "string") &&
    typeof v.createdAtMillis === "number" &&
    typeof v.lastLoginAtMillis === "number" &&
    typeof v.lastRefreshAtMillis === "number" &&
    typeof v.counterExists === "boolean" &&
    (v.counterVersion === undefined || typeof v.counterVersion === "number") &&
    typeof v.deletionRequestStatus === "string";
}
