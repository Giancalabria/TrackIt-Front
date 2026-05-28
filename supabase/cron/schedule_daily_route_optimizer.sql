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

select cron.schedule(
  'daily-route-optimizer',
  '0 2 * * *',
  $$
  select net.http_post(
    url := 'https://PROJECT_REF.supabase.co/functions/v1/daily-route-optimizer',
    headers := '{"Authorization":"Bearer SERVICE_ROLE_KEY","Content-Type":"application/json"}'::jsonb,
    body := json_build_object('targetDate', (now() + interval '1 day')::date)::text
  );
  $$
);

