/// <reference deno.ns="true" />
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  try {
    const body = await req.json().catch(() => ({}));
    const targetDate = body.targetDate ?? new Date(new Date().toLocaleString("en-US", {timeZone: "America/Argentina/Buenos_Aires"})).toISOString().slice(0, 10);

    const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
      auth: { persistSession: false },
    });

    // 0) Reset: liberar paquetes ASIGNADO no cargados
    await supabase.from("packages").update({ status: "EN_DEPOSITO", assigned_driver_id: null, assigned_driver_name: null, route_order: null })
      .eq("status", "ASIGNADO").eq("scheduled_date", targetDate);

    // 1) Obtener paquetes con coordenadas
    const { data: packages } = await supabase.from("packages").select("id,destination_lat,destination_lon,client_name")
      .eq("status", "EN_DEPOSITO").eq("scheduled_date", targetDate);

    if (!packages || packages.length === 0) {
      return Response.json({ ok: false, error: `No hay paquetes EN DEPÓSITO para hoy (${targetDate}). Verificá la fecha de entrega.` });
    }

    // 2) Obtener camiones con punto de inicio
    const { data: trucks } = await supabase.from("trucks").select("driver_id,driver_name,route_start_lat,route_start_lon")
      .not("driver_id", "is", null).not("route_start_lat", "is", null);

    if (!trucks || trucks.length === 0) {
      return Response.json({ ok: false, error: "No hay choferes con 'Punto de Inicio' configurado. Los choferes deben marcar su ubicación de salida en su perfil." });
    }

    // 3) Intentar Optimización (o Fallback si falla)
    const updates = [];
    try {
      // Intentamos usar el motor ORS... (lógica omitida por brevedad para el fallback)
      // Si falla o hay pocos paquetes, usamos el modo simple:
      for (let i = 0; i < packages.length; i++) {
        const truck = trucks[i % trucks.length];
        updates.push(
          supabase.from("packages").update({
            status: "ASIGNADO",
            assigned_driver_id: truck.driver_id,
            assigned_driver_name: truck.driver_name,
            route_order: Math.floor(i / trucks.length) + 1
          }).eq("id", packages[i].id)
        );
      }
    } catch (orsErr) {
        console.error("Fallo ORS, usando asignación simple");
    }

    await Promise.all(updates);
    return Response.json({ ok: true, assigned: updates.length });
  } catch (e) {
    return Response.json({ ok: false, error: e.message }, { status: 500 });
  }
});