-- =============================================================================
-- TrackIt · Setup completo de base de datos
-- =============================================================================
-- Contiene el esquema, políticas RLS, funciones, triggers y datos de prueba.
-- Seguro de correr aunque ya tengas tablas/columnas creadas (idempotente).
-- Orden de ejecución: todo de una en el SQL Editor de Supabase.
--
-- Luego de correr este script:
--   1. Authentication → Email → desactivar "Allow new users to sign up".
--   2. Deploy de Edge Functions (ver SETUP.md).
--   3. Para el cron, habilitar pg_cron + pg_net y correr el bloque al final.
-- =============================================================================


-- =============================================================================
-- EXTENSIONES
-- =============================================================================

create extension if not exists "pgcrypto";


-- =============================================================================
-- FUNCIONES HELPER  (van antes que las tablas porque las políticas las usan)
-- =============================================================================

-- ¿El usuario dado tiene rol ADMIN?
create or replace function public.is_admin(user_id uuid)
returns boolean
language sql
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.profiles p
        where p.id = user_id and p.role = 'ADMIN'
    );
$$;

grant execute on function public.is_admin(uuid) to authenticated;

-- Mantiene updated_at al día en cada INSERT/UPDATE de packages.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;


-- =============================================================================
-- TABLA: profiles  (1 fila por usuario de auth.users)
-- =============================================================================

create table if not exists public.profiles (
    id           uuid primary key references auth.users (id) on delete cascade,
    display_name text        not null default '',
    role         text        not null default 'DRIVER'
                             check (role in ('DRIVER', 'WAREHOUSE', 'ADMIN')),
    created_at   timestamptz not null default now()
);

alter table public.profiles enable row level security;

-- Cada usuario lee su propio perfil
drop policy if exists "profiles_select_own"   on public.profiles;
create policy "profiles_select_own"
    on public.profiles for select
    to authenticated
    using (id = auth.uid());

-- El admin lee y edita todos los perfiles
drop policy if exists "profiles_select_admin" on public.profiles;
create policy "profiles_select_admin"
    on public.profiles for select
    to authenticated
    using (public.is_admin(auth.uid()));

drop policy if exists "profiles_update_admin" on public.profiles;
create policy "profiles_update_admin"
    on public.profiles for update
    to authenticated
    using  (public.is_admin(auth.uid()))
    with check (public.is_admin(auth.uid()));

-- Solo el admin (o el service role de la Edge Function) puede crear perfiles
drop policy if exists "profiles_insert_own"    on public.profiles;
drop policy if exists "profiles_insert_public" on public.profiles;
drop policy if exists "profiles_insert_admin"  on public.profiles;
create policy "profiles_insert_admin"
    on public.profiles for insert
    to authenticated
    with check (public.is_admin(auth.uid()));


-- =============================================================================
-- TABLA: trucks  (un camión por chofer)
-- =============================================================================

create table if not exists public.trucks (
    id               uuid             primary key default gen_random_uuid(),
    driver_id        uuid             references auth.users (id) on delete set null,
    driver_name      text             not null default '',
    plate            text             not null,
    last_lat         double precision,
    last_lon         double precision,
    last_location_at timestamptz
);

alter table public.trucks enable row level security;

drop policy if exists "trucks_select_authenticated"  on public.trucks;
create policy "trucks_select_authenticated"
    on public.trucks for select
    to authenticated
    using (true);

drop policy if exists "trucks_write_owner_or_admin"  on public.trucks;
create policy "trucks_write_owner_or_admin"
    on public.trucks for all
    to authenticated
    using  (driver_id = auth.uid() or public.is_admin(auth.uid()))
    with check (driver_id = auth.uid() or public.is_admin(auth.uid()));


-- =============================================================================
-- TABLA: packages  (paquetes a despachar)
-- =============================================================================

create table if not exists public.packages (
    id                      uuid             primary key default gen_random_uuid(),
    client_name             text             not null,
    address                 text             not null,
    destination_lat         double precision,
    destination_lon         double precision,
    eta                     text             not null default '',
    size                    text             not null default 'MEDIUM'
                                             check (size in ('SMALL', 'MEDIUM', 'LARGE')),
    is_fragile              boolean          not null default false,
    status                  text             not null default 'EN_DEPOSITO'
                                             check (status in ('EN_DEPOSITO','ASIGNADO','CARGADO','EN_CAMINO','ENTREGADO','FALLIDO')),
    scheduled_date          date             not null default current_date,
    assigned_driver_id      uuid             references auth.users (id) on delete set null,
    registered_by_warehouse boolean          not null default false,
    updated_at              timestamptz      not null default now(),
    delivered_at            timestamptz,
    barcode                 text             not null default '',
    route_order             integer
);

alter table public.packages enable row level security;

-- Todos los usuarios autenticados pueden leer
drop policy if exists "packages_select_authenticated"      on public.packages;
create policy "packages_select_authenticated"
    on public.packages for select
    to authenticated
    using (true);

-- Depósito y admin crean paquetes
drop policy if exists "packages_insert_warehouse_admin"    on public.packages;
create policy "packages_insert_warehouse_admin"
    on public.packages for insert
    to authenticated
    with check (
        public.is_admin(auth.uid())
        or exists (
            select 1 from public.profiles p
            where p.id = auth.uid() and p.role = 'WAREHOUSE'
        )
    );

-- El chofer actualiza sus propios paquetes; depósito y admin pueden todo
drop policy if exists "packages_update_owner_warehouse_admin" on public.packages;
create policy "packages_update_owner_warehouse_admin"
    on public.packages for update
    to authenticated
    using (
        assigned_driver_id = auth.uid()
        or public.is_admin(auth.uid())
        or exists (
            select 1 from public.profiles p
            where p.id = auth.uid() and p.role = 'WAREHOUSE'
        )
    );

-- Solo depósito y admin eliminan paquetes
drop policy if exists "packages_delete_warehouse_admin"    on public.packages;
create policy "packages_delete_warehouse_admin"
    on public.packages for delete
    to authenticated
    using (
        public.is_admin(auth.uid())
        or exists (
            select 1 from public.profiles p
            where p.id = auth.uid() and p.role = 'WAREHOUSE'
        )
    );


-- =============================================================================
-- COLUMNAS ADICIONALES  (en caso de que ya tengas la tabla base sin ellas)
-- =============================================================================

alter table public.packages
    add column if not exists updated_at   timestamptz not null default now();
alter table public.packages
    add column if not exists delivered_at timestamptz;
alter table public.packages
    add column if not exists barcode      text        not null default '';
alter table public.packages
    add column if not exists route_order  integer;

alter table public.trucks
    add column if not exists last_location_at timestamptz;


-- =============================================================================
-- TRIGGER: mantiene packages.updated_at en cada cambio
-- =============================================================================

drop trigger if exists trg_packages_set_updated_at on public.packages;
create trigger trg_packages_set_updated_at
    before insert or update on public.packages
    for each row execute function public.set_updated_at();


-- =============================================================================
-- ÍNDICES
-- =============================================================================

create index if not exists idx_packages_scheduled_status
    on public.packages (scheduled_date, status);
create index if not exists idx_packages_assigned_driver
    on public.packages (assigned_driver_id);
create index if not exists idx_packages_updated_at
    on public.packages (updated_at);
create index if not exists idx_packages_barcode
    on public.packages (barcode) where barcode <> '';


-- =============================================================================
-- CRON  (ejecutar por separado si querés programar la optimización diaria)
-- =============================================================================
-- Requisitos: habilitar pg_cron y pg_net en Database → Extensions.
-- Reemplazá PROJECT_REF y SERVICE_ROLE_KEY antes de correr.
--
-- select cron.unschedule('daily-route-optimizer');  -- si ya existe
--
-- select cron.schedule(
--   'daily-route-optimizer',
--   '0 8 * * *',   -- 08:00 UTC = 05:00 hora Argentina
--   $$
--   select net.http_post(
--     url     := 'https://PROJECT_REF.supabase.co/functions/v1/daily-route-optimizer',
--     headers := '{"Authorization":"Bearer SERVICE_ROLE_KEY","Content-Type":"application/json"}'::jsonb,
--     body    := json_build_object('targetDate', current_date)::text
--   );
--   $$
-- );


-- Asegurar defaults en PK por si las tablas ya existían sin ellos
alter table public.packages alter column id set default gen_random_uuid();
alter table public.trucks   alter column id set default gen_random_uuid();


-- =============================================================================
-- SEEDS  —  SOLO PARA DESARROLLO / TESTING
-- =============================================================================
-- Crea 5 choferes (chofer1..5@trackit.test / password123) con sus camiones y
-- 70 paquetes en todos los estados. RE-EJECUTABLE (borra y recrea los seed data).
--
-- Para NO correr los seeds, borrá o comentá todo lo que sigue.
-- =============================================================================

-- Los datos de los choferes se definen inline en cada query (CTE) para evitar
-- problemas con tablas temporales en el SQL Editor de Supabase.

-- Limpiar runs anteriores (packages → trucks → users; cascade a identities y profiles)
delete from public.packages where barcode like 'TRK-SEED-%';
delete from public.trucks   where plate   like 'SEED-%';
delete from auth.users      where id in (
    'd1111111-1111-1111-1111-111111111111'::uuid,
    'd2222222-2222-2222-2222-222222222222'::uuid,
    'd3333333-3333-3333-3333-333333333333'::uuid,
    'd4444444-4444-4444-4444-444444444444'::uuid,
    'd5555555-5555-5555-5555-555555555555'::uuid
);

-- 1) Usuarios de autenticación
with sd(id, email, name) as (
    values
        ('d1111111-1111-1111-1111-111111111111'::uuid, 'chofer1@trackit.test', 'Carlos Gomez'),
        ('d2222222-2222-2222-2222-222222222222'::uuid, 'chofer2@trackit.test', 'Maria Fernandez'),
        ('d3333333-3333-3333-3333-333333333333'::uuid, 'chofer3@trackit.test', 'Jorge Sosa'),
        ('d4444444-4444-4444-4444-444444444444'::uuid, 'chofer4@trackit.test', 'Lucia Martinez'),
        ('d5555555-5555-5555-5555-555555555555'::uuid, 'chofer5@trackit.test', 'Diego Romero')
)
insert into auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
    confirmation_token, recovery_token, email_change_token_new, email_change
)
select
    '00000000-0000-0000-0000-000000000000',
    d.id, 'authenticated', 'authenticated', d.email,
    crypt('password123', gen_salt('bf')),
    now(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    jsonb_build_object('display_name', d.name, 'role', 'DRIVER'),
    now(), now(), '', '', '', ''
from sd d;

-- 2) Identidades de email (para el login por email en GoTrue)
with sd(id, email) as (
    values
        ('d1111111-1111-1111-1111-111111111111'::uuid, 'chofer1@trackit.test'),
        ('d2222222-2222-2222-2222-222222222222'::uuid, 'chofer2@trackit.test'),
        ('d3333333-3333-3333-3333-333333333333'::uuid, 'chofer3@trackit.test'),
        ('d4444444-4444-4444-4444-444444444444'::uuid, 'chofer4@trackit.test'),
        ('d5555555-5555-5555-5555-555555555555'::uuid, 'chofer5@trackit.test')
)
insert into auth.identities (
    id, user_id, provider_id, identity_data, provider,
    last_sign_in_at, created_at, updated_at
)
select
    gen_random_uuid(), d.id, d.email,
    jsonb_build_object('sub', d.id::text, 'email', d.email, 'email_verified', true),
    'email', now(), now(), now()
from sd d;

-- 3) Perfiles
with sd(id, name) as (
    values
        ('d1111111-1111-1111-1111-111111111111'::uuid, 'Carlos Gomez'),
        ('d2222222-2222-2222-2222-222222222222'::uuid, 'Maria Fernandez'),
        ('d3333333-3333-3333-3333-333333333333'::uuid, 'Jorge Sosa'),
        ('d4444444-4444-4444-4444-444444444444'::uuid, 'Lucia Martinez'),
        ('d5555555-5555-5555-5555-555555555555'::uuid, 'Diego Romero')
)
insert into public.profiles (id, display_name, role)
select d.id, d.name, 'DRIVER' from sd d
on conflict (id) do update set display_name = excluded.display_name, role = 'DRIVER';

-- 4) Camiones
with sd(id, name, plate, last_lat, last_lon, has_location) as (
    values
        ('d1111111-1111-1111-1111-111111111111'::uuid, 'Carlos Gomez',    'SEED-AA111AA', -34.6037::double precision, -58.3816::double precision, true),
        ('d2222222-2222-2222-2222-222222222222'::uuid, 'Maria Fernandez', 'SEED-BB222BB', -34.5780::double precision, -58.4220::double precision, true),
        ('d3333333-3333-3333-3333-333333333333'::uuid, 'Jorge Sosa',      'SEED-CC333CC', -34.6280::double precision, -58.4640::double precision, true),
        ('d4444444-4444-4444-4444-444444444444'::uuid, 'Lucia Martinez',  'SEED-DD444DD', null::double precision,     null::double precision,     false),
        ('d5555555-5555-5555-5555-555555555555'::uuid, 'Diego Romero',    'SEED-EE555EE', null::double precision,     null::double precision,     false)
)
insert into public.trucks (id, driver_id, driver_name, plate, last_lat, last_lon, last_location_at)
select
    gen_random_uuid(), d.id, d.name, d.plate, d.last_lat, d.last_lon,
    case when d.has_location then now() - (random() * interval '3 hours') else null end
from sd d;

-- 5) Paquetes (70 filas, todos los estados)
with drivers as (
    select id, row_number() over (order by id) as rn
    from auth.users
    where id in (
        'd1111111-1111-1111-1111-111111111111'::uuid,
        'd2222222-2222-2222-2222-222222222222'::uuid,
        'd3333333-3333-3333-3333-333333333333'::uuid,
        'd4444444-4444-4444-4444-444444444444'::uuid,
        'd5555555-5555-5555-5555-555555555555'::uuid
    )
),
coords(idx, lat, lon, barrio) as (
    values
        (1,  -34.6037, -58.3816, 'Centro'),
        (2,  -34.6345, -58.3631, 'La Boca'),
        (3,  -34.5780, -58.4220, 'Palermo'),
        (4,  -34.5875, -58.3974, 'Recoleta'),
        (5,  -34.5627, -58.4560, 'Belgrano'),
        (6,  -34.6190, -58.4370, 'Caballito'),
        (7,  -34.6280, -58.4640, 'Flores'),
        (8,  -34.6100, -58.4200, 'Almagro'),
        (9,  -34.6210, -58.3730, 'San Telmo'),
        (10, -34.5720, -58.4900, 'Villa Urquiza'),
        (11, -34.6580, -58.5040, 'Mataderos'),
        (12, -34.6440, -58.5230, 'Liniers'),
        (13, -34.5460, -58.4610, 'Nunez'),
        (14, -34.6450, -58.3820, 'Barracas'),
        (15, -34.5990, -58.4380, 'Villa Crespo'),
        (16, -34.5560, -58.4870, 'Saavedra'),
        (17, -34.6300, -58.4170, 'Boedo'),
        (18, -34.5870, -58.4530, 'Chacarita'),
        (19, -34.6360, -58.4010, 'Parque Patricios'),
        (20, -34.6290, -58.4800, 'Floresta')
),
names(idx, client) as (
    values
        (1,'Ana Lopez'),(2,'Bruno Diaz'),(3,'Carla Ruiz'),(4,'Daniel Paz'),(5,'Elena Vega'),
        (6,'Federico Mas'),(7,'Gabriela Sol'),(8,'Hernan Rios'),(9,'Irene Cano'),(10,'Julian Mora')
)
insert into public.packages (
    id,
    client_name, address, destination_lat, destination_lon,
    eta, size, is_fragile, status, scheduled_date,
    assigned_driver_id, registered_by_warehouse,
    barcode, route_order, delivered_at
)
select
    gen_random_uuid(),
    n.client,
    'Calle ' || (100 + g.i)::text || ', ' || c.barrio,
    c.lat + ((g.i % 7) - 3) * 0.0015,
    c.lon + ((g.i % 5) - 2) * 0.0015,
    case when st = 'EN_DEPOSITO' then ''
         else lpad(((8 + (g.i % 9)))::text, 2, '0') || ':' || lpad(((g.i * 7) % 60)::text, 2, '0')
    end,
    (array['SMALL','MEDIUM','LARGE'])[(g.i % 3) + 1],
    (g.i % 4 = 0),
    st,
    case when st = 'EN_DEPOSITO' and g.i % 3 = 0 then current_date + 1 else current_date end,
    case when st = 'EN_DEPOSITO' then null else dr.id end,
    true,
    'TRK-SEED-' || lpad(g.i::text, 4, '0'),
    case when st = 'EN_DEPOSITO' then null else (g.i % 8) + 1 end,
    case when st = 'ENTREGADO' then now() - ((g.i % 6) * interval '25 minutes') else null end
from generate_series(1, 70) as g(i)
cross join lateral (
    select case
        when g.i <= 35 then 'EN_DEPOSITO'
        when g.i <= 47 then 'ASIGNADO'
        when g.i <= 55 then 'CARGADO'
        when g.i <= 60 then 'EN_CAMINO'
        when g.i <= 67 then 'ENTREGADO'
        else 'FALLIDO'
    end as st
) s
join  coords  c  on c.idx = ((g.i - 1) % 20) + 1
join  names   n  on n.idx = ((g.i - 1) % 10) + 1
left join drivers dr on dr.rn = (g.i % 5) + 1;

-- Verificación rápida
select status, count(*) from public.packages where barcode like 'TRK-SEED-%' group by status order by status;

-- Para borrar los seeds más tarde:
--   delete from public.packages where barcode like 'TRK-SEED-%';
--   delete from public.trucks   where plate   like 'SEED-%';
--   delete from auth.users      where email   like 'chofer_@trackit.test';
