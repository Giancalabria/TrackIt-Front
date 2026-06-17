-- Schedules the daily route optimization Edge Function.
--
-- Prereqs:
-- - Enable extensions: pg_cron, pg_net (in Supabase dashboard)
-- - Deploy Edge Function: daily-route-optimizer
-- - Set Edge Function secrets:
--   - ORS_API_KEY
--   - (optional) WAREHOUSE_LAT, WAREHOUSE_LON
--
-- NOTE: Replace PROJECT_REF and SERVICE_ROLE_KEY placeholders.
--
-- Horario: 08:00 UTC = 05:00 hora Argentina (UTC-3). Optimiza los paquetes de ESE
-- MISMO dia (current_date), que fueron ingresados el dia anterior con scheduled_date = hoy.
-- Re-ejecutable: la Edge Function resetea los ASIGNADO del dia y rebalancea (no toca CARGADO+).

-- Si ya existe un schedule con este nombre, primero desprogramalo:
--   select cron.unschedule('daily-route-optimizer');

select cron.schedule(
  'daily-route-optimizer',
  '0 8 * * *',
  $$
  select net.http_post(
    url := 'https://PROJECT_REF.supabase.co/functions/v1/daily-route-optimizer',
    headers := '{"Authorization":"Bearer SERVICE_ROLE_KEY","Content-Type":"application/json"}'::jsonb,
    body := json_build_object('targetDate', current_date)::text
  );
  $$
);

