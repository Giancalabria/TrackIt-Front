/// <reference deno.ns="true" />

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

type CreateUserRequest = {
  email?: string;
  password?: string;
  displayName?: string;
  role?: "DRIVER" | "WAREHOUSE" | "ADMIN";
};

const VALID_ROLES = ["DRIVER", "WAREHOUSE", "ADMIN"];

function requireEnv(name: string): string {
  const v = Deno.env.get(name);
  if (!v) throw new Error(`Missing env: ${name}`);
  return v;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  try {
    if (req.method !== "POST") {
      return json({ ok: false, error: "method_not_allowed" }, 405);
    }

    const supabaseUrl = requireEnv("SUPABASE_URL");
    const serviceRoleKey = requireEnv("SUPABASE_SERVICE_ROLE_KEY");
    const anonKey = requireEnv("SUPABASE_ANON_KEY");

    // 1) Require the caller's JWT.
    const authHeader = req.headers.get("Authorization") ?? "";
    const jwt = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : "";
    if (!jwt) {
      return json({ ok: false, error: "missing_auth" }, 401);
    }

    // Admin client (service role) bypasses RLS for reads/writes.
    const admin = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    // 2) Resolve the caller from the JWT and verify they are an ADMIN.
    const callerClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: `Bearer ${jwt}` } },
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const { data: callerData, error: callerErr } = await callerClient.auth.getUser();
    if (callerErr || !callerData?.user) {
      return json({ ok: false, error: "invalid_token" }, 401);
    }

    const { data: callerProfile } = await admin
      .from("profiles")
      .select("role")
      .eq("id", callerData.user.id)
      .single();

    if (callerProfile?.role !== "ADMIN") {
      return json({ ok: false, error: "forbidden_not_admin" }, 403);
    }

    // 3) Validate input.
    const body = (await req.json().catch(() => ({}))) as CreateUserRequest;
    const email = (body.email ?? "").trim().toLowerCase();
    const password = body.password ?? "";
    const displayName = (body.displayName ?? "").trim();
    const role = body.role ?? "DRIVER";

    if (!email || !password) {
      return json({ ok: false, error: "missing_email_or_password" }, 400);
    }
    if (password.length < 6) {
      return json({ ok: false, error: "weak_password" }, 400);
    }
    if (!VALID_ROLES.includes(role)) {
      return json({ ok: false, error: "invalid_role" }, 400);
    }

    // 4) Create the auth user with the service role (does NOT touch the admin's session).
    const { data: created, error: createErr } = await admin.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
      user_metadata: { display_name: displayName, role },
    });

    if (createErr || !created?.user) {
      return json({ ok: false, error: createErr?.message ?? "create_failed" }, 400);
    }

    // 5) Insert the profile row. Roll back the auth user if it fails.
    const { error: profileErr } = await admin.from("profiles").insert({
      id: created.user.id,
      display_name: displayName,
      role,
    });

    if (profileErr) {
      await admin.auth.admin.deleteUser(created.user.id);
      return json({ ok: false, error: `profile_insert_failed: ${profileErr.message}` }, 400);
    }

    return json({
      ok: true,
      user: { id: created.user.id, email, displayName, role },
    });
  } catch (e) {
    return json({ ok: false, error: String(e) }, 500);
  }
});
