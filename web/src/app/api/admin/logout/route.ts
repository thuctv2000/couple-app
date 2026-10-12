import { NextResponse } from "next/server";
import { readAdminConfig, validMutationOrigin } from "@/features/admin/server/config";

export async function POST(request: Request) {
  const config = readAdminConfig(process.env);
  if (!config) return NextResponse.json({ error: "not_configured" }, { status: 503 });
  if (!validMutationOrigin(request, config)) return NextResponse.json({ error: "invalid_origin" }, { status: 403 });
  const response = NextResponse.redirect(`${config.webOrigin}/admin/login`, 303);
  response.headers.set("Cache-Control", "no-store");
  response.cookies.set(config.cookieName, "", { httpOnly: true, secure: config.secure, sameSite: "strict", path: "/", maxAge: 0 });
  return response;
}
