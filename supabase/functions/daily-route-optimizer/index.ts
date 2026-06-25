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

    // 0) Reset: liberamos los ASIGNADO que no han sido cargados aún para re-balancear
    await supabase
      .from("packages")
      .update({ status: "EN_DEPOSITO", assigned_driver_id: null, assigned_driver_name: null, route_order: null })
      .eq("status", "ASIGNADO")
      .eq("scheduled_date", targetDate);

    // 1) Fetch paquetes
    const { data: packages } = await supabase
      .from("packages")
      .select("id,destination_lat,destination_lon")
      .eq("status", "EN_DEPOSITO")
      .eq("scheduled_date", targetDate);

    const jobs = (packages ?? [])
      .filter((p) => p.destination_lat != null && p.destination_lon != null)
      .map((p, idx) => ({
        jobId: idx + 1,
        packageId: p.id as string,
        location: [Number(p.destination_lon), Number(p.destination_lat)] as [number, number],
      }));

    if (jobs.length <= 10) {
      return Response.json({
        ok: false,
        error: "Se necesitan más de 10 paquetes para generar rutas automáticamente."
      });
    }

    // 2) Fetch camiones (traemos el ID y el NOMBRE del chofer)
    const { data: trucks } = await supabase
      .from("trucks")
      .select("id,driver_id,driver_name,route_start_lat,route_start_lon")
      .not("driver_id", "is", null)
      .not("route_start_lat", "is", null);

    const maxDrivers = Math.floor(jobs.length / 5);
    const driversToUse = Math.min((trucks ?? []).length, maxDrivers);

    if (driversToUse === 0) {
      return Response.json({ ok: false, error: "No hay choferes listos para rutear (mínimo 5 paquetes por persona)." }, { status: 400 });
    }

    const vehicles = (trucks ?? []).slice(0, driversToUse).map((t, idx) => ({
      vehicleId: idx + 1,
      driverId: t.driver_id as string,
      driverName: t.driver_name as string,
      start: [Number(t.route_start_lon), Number(t.route_start_lat)] as [number, number],
      capacity: [Math.ceil(jobs.length / driversToUse) + 1]
    }));

    // 3) Llamada a ORS
    const orsPayload: OrsOptimizationRequest = {
      vehicles: vehicles.map(v => ({ id: v.vehicleId, start: v.start, end: v.start, capacity: v.capacity })),
      jobs: jobs.map(j => ({ id: j.jobId, location: j.location, amount: [1] }))
    };

    const orsRes = await fetch("https://api.openrouteservice.org/optimization", {
      method: "POST",
      headers: { Authorization: orsApiKey, "Content-Type": "application/json" },
      body: JSON.stringify(orsPayload)
    });

    const orsJson = await orsRes.json() as OrsOptimizationResponse;

    // 4) Guardar asignaciones con NOMBRE de chofer
    const jobIdToPackageId = new Map(jobs.map(j => [j.jobId, j.packageId]));
    const vehicleToDriver = new Map(vehicles.map(v => [v.vehicleId, { id: v.driverId, name: v.driverName }]));

    const updates = [];
    for (const route of orsJson.routes ?? []) {
      const driver = vehicleToDriver.get(route.vehicle);
      if (!driver) continue;

      let order = 1;
      for (const step of route.steps ?? []) {
        if (step.type !== "job" || !step.job) continue;
        const pkgId = jobIdToPackageId.get(step.job);
        updates.push(
          supabase.from("packages").update({
            status: "ASIGNADO",
            assigned_driver_id: driver.id,
            assigned_driver_name: driver.name,
            route_order: order++
          }).eq("id", pkgId)
        );
      }
    }

    await Promise.all(updates);

    return Response.json({ ok: true, assigned: updates.length });
  } catch (e) {
    return Response.json({ ok: false, error: e.message }, { status: 500 });
  }
});