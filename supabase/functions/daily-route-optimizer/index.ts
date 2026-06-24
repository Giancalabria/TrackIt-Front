/// <reference deno.ns="true" />

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

type OptimizeRequest = {
  targetDate?: string;
};

type OrsOptimizationRequest = {
  vehicles: Array<{
    id: number;
    start: [number, number];
    end?: [number, number];
    capacity?: number[];
  }>;
  jobs: Array<{
    id: number;
    location: [number, number];
    amount?: number[];
  }>;
};

type OrsOptimizationResponse = {
  routes?: Array<{
    vehicle: number;
    steps?: Array<{
      type: string;
      job?: number;
    }>;
  }>;
  unassigned?: Array<{ id: number; reason?: string }>;
};

function requireEnv(name: string): string {
  const v = Deno.env.get(name);
  if (!v) throw new Error(`Missing env: ${name}`);
  return v;
}

Deno.serve(async (req) => {
  try {
    if (req.method !== "POST") {
      return new Response("Method Not Allowed", { status: 405 });
    }

    const body = (await req.json().catch(() => ({}))) as OptimizeRequest;
    const targetDate = body.targetDate ?? new Date().toISOString().slice(0, 10);

    const supabaseUrl = requireEnv("SUPABASE_URL");
    const serviceRoleKey = requireEnv("SUPABASE_SERVICE_ROLE_KEY");
    const orsApiKey = requireEnv("ORS_API_KEY");

    const supabase = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false },
    });

    // 0) Re-run support: free previously ASIGNADO (not yet loaded) packages for this date
    //    so they can be rebalanced. Never touch CARGADO or beyond.
    const { error: resetErr } = await supabase
      .from("packages")
      .update({ status: "EN_DEPOSITO", assigned_driver_id: null, route_order: null })
      .eq("status", "ASIGNADO")
      .eq("scheduled_date", targetDate);
    if (resetErr) throw resetErr;

    // 1) Fetch packages to assign (EN_DEPOSITO for targetDate)
    const { data: packages, error: pkgErr } = await supabase
      .from("packages")
      .select("id,destination_lat,destination_lon,status,scheduled_date")
      .eq("status", "EN_DEPOSITO")
      .eq("scheduled_date", targetDate);
    if (pkgErr) throw pkgErr;

    const jobs = (packages ?? [])
      .filter((p) => p.destination_lat != null && p.destination_lon != null)
      .map((p, idx) => ({
        jobId: idx + 1,
        packageId: p.id as string,
        location: [Number(p.destination_lon), Number(p.destination_lat)] as [number, number],
      }));

    if (jobs.length === 0) {
      return Response.json({ ok: true, targetDate, assigned: 0, reason: "no_jobs" });
    }

    // 2) Fetch vehicles with a mandatory route start configured by each driver.
    const { data: trucks, error: trkErr } = await supabase
      .from("trucks")
      .select("id,driver_id,route_start_lat,route_start_lon")
      .not("driver_id", "is", null)
      .not("route_start_lat", "is", null)
      .not("route_start_lon", "is", null);
    if (trkErr) throw trkErr;

    const vehicles = (trucks ?? []).map((t, idx) => ({
      vehicleId: idx + 1,
      driverId: t.driver_id as string,
      start: [Number(t.route_start_lon), Number(t.route_start_lat)] as [number, number],
      end: [Number(t.route_start_lon), Number(t.route_start_lat)] as [number, number],
    }));

    if (vehicles.length === 0) {
      return Response.json(
        { ok: false, targetDate, error: "no_vehicles_with_route_start" },
        { status: 400 },
      );
    }

    // 3) Call ORS Optimization (VROOM)
    const orsPayload: OrsOptimizationRequest = {
      vehicles: vehicles.map((v) => ({
        id: v.vehicleId,
        start: v.start,
        end: v.end,
      })),
      jobs: jobs.map((j) => ({
        id: j.jobId,
        location: j.location,
        amount: [1],
      })),
    };

    const orsRes = await fetch("https://api.openrouteservice.org/optimization", {
      method: "POST",
      headers: {
        Authorization: orsApiKey,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(orsPayload),
    });

    if (!orsRes.ok) {
      const text = await orsRes.text();
      return Response.json(
        { ok: false, targetDate, error: "ors_failed", status: orsRes.status, details: text },
        { status: 502 },
      );
    }

    const orsJson = (await orsRes.json()) as OrsOptimizationResponse;

    const jobIdToPackageId = new Map<number, string>();
    for (const j of jobs) jobIdToPackageId.set(j.jobId, j.packageId);

    const vehicleIdToDriverId = new Map<number, string>();
    for (const v of vehicles) vehicleIdToDriverId.set(v.vehicleId, v.driverId);

    // 4) Apply assignments (status=ASIGNADO, assigned_driver_id=driver, route_order=visit order)
    const updates: Array<Promise<unknown>> = [];
    for (const route of orsJson.routes ?? []) {
      const driverId = vehicleIdToDriverId.get(route.vehicle);
      if (!driverId) continue;

      let visitOrder = 1;
      for (const step of route.steps ?? []) {
        if (step.type !== "job" || step.job == null) continue;
        const packageId = jobIdToPackageId.get(step.job);
        if (!packageId) continue;

        updates.push(
          supabase
            .from("packages")
            .update({
              status: "ASIGNADO",
              assigned_driver_id: driverId,
              route_order: visitOrder,
            })
            .eq("id", packageId),
        );
        visitOrder++;
      }
    }

    await Promise.all(updates);

    return Response.json({
      ok: true,
      targetDate,
      vehicles: vehicles.length,
      jobs: jobs.length,
      assigned: updates.length,
      unassigned: orsJson.unassigned?.length ?? 0,
    });
  } catch (e) {
    return Response.json(
      { ok: false, error: String(e?.message ?? e) },
      { status: 500 },
    );
  }
});

