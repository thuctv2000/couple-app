import test from "node:test";
import assert from "node:assert/strict";
import { readAdminConfig, readAdminLoginConfig, validMutationOrigin } from "../src/features/admin/server/config.ts";
import { verifyAdmin } from "../src/features/admin/server/verify.ts";

const localEnv = { APP_ENV: "local", ADMIN_WEB_ORIGIN: "http://127.0.0.1:13000", ADMIN_API_ORIGIN: "http://127.0.0.1:18080" };
const config = readAdminConfig(localEnv)!;
test("origins fail closed and insecure origins require explicit local loopback", () => {
  assert.ok(config); assert.equal(config.secure, false);
  assert.equal(readAdminConfig({ ...localEnv, APP_ENV: "production" }), null);
  for (const origin of ["http://example.com", "https://user:password@example.com", "https://example.com/path", "https://example.com/?token=x", "file:///tmp"]) assert.equal(readAdminConfig({ ...localEnv, ADMIN_API_ORIGIN: origin }), null);
  const production = readAdminConfig({ ADMIN_WEB_ORIGIN: "https://admin.example.com", ADMIN_API_ORIGIN: "https://api.example.com" })!;
  assert.equal(production.cookieName, "__Host-couple_admin"); assert.equal(production.secure, true);
  assert.equal(validMutationOrigin(new Request(config.webOrigin, { headers: { origin: config.webOrigin } }), config), true);
  for (const origin of ["https://attacker.example", "null", ""]) assert.equal(validMutationOrigin(new Request(config.webOrigin, { headers: { origin } }), config), false);
});
test("Firebase emulator cannot be mixed with a real project or production mode", () => {
  const env = { ...localEnv, NEXT_PUBLIC_FIREBASE_API_KEY: "demo-key", NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN: "demo-couple-app.firebaseapp.com", NEXT_PUBLIC_FIREBASE_PROJECT_ID: "demo-couple-app", NEXT_PUBLIC_FIREBASE_APP_ID: "demo-app", NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_URL: "http://127.0.0.1:19099" };
  assert.ok(readAdminLoginConfig(env));
  assert.equal(readAdminLoginConfig({ ...env, APP_ENV: "production" }), null);
  assert.equal(readAdminLoginConfig({ ...env, NEXT_PUBLIC_FIREBASE_PROJECT_ID: "real-project" }), null);
  assert.equal(readAdminLoginConfig({ ...env, NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_URL: "" }), null);
});
test("only verified admin payload grants access; requests never cache or follow redirects", async () => {
  for (const [status, body, expected] of [[200, {role:"admin"}, "allowed"], [200, {role:"user"}, "unavailable"], [401, {}, "unauthenticated"], [403, {}, "forbidden"], [500, {}, "unavailable"], [302, {}, "unavailable"]] as const) {
    const fetcher: typeof fetch = async (input, options) => {
      assert.equal(input, `${config.apiOrigin}/v1/admin/session`);
      assert.equal(options?.cache, "no-store"); assert.equal(options?.redirect, "error");
      assert.deepEqual(options?.headers, {Authorization:"Bearer token.value"});
      return new Response(JSON.stringify(body), {status});
    };
    assert.equal(await verifyAdmin(config, "token.value", fetcher), expected);
  }
  assert.equal(await verifyAdmin(config, "token.value", async () => {throw new Error("private upstream text");}), "unavailable");
  for (const token of [undefined, "", "a\r\nb", "x".repeat(3801)]) assert.equal(await verifyAdmin(config, token, async () => {throw new Error("must not fetch");}), "unauthenticated");
});
