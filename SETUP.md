# TrackIt — Puesta en marcha (checklist en orden)

Guía paso a paso de **todo lo que hay que hacer** para dejar la app funcionando, en el orden
correcto. Incluye los comandos. Pensado para Windows / PowerShell (los comandos `supabase` y
`gradlew` son equivalentes en macOS/Linux usando `./gradlew`).

> Arquitectura: **offline-first** (Room como fuente de verdad + sync con Supabase por WorkManager).
> Alta de usuarios **solo por admin**. Optimización de rutas diaria por Edge Function + cron.

---

## 0. Requisitos previos

- Android Studio (Ladybug o superior) + JDK 17.
- Un proyecto de **Supabase** (anotá `PROJECT_REF`, `URL`, `anon key`, `service_role key`).
- **Supabase CLI**: https://supabase.com/docs/guides/cli  (`supabase --version`).
- API key de **OpenRouteService** (https://openrouteservice.org/).
- Un dispositivo/emulador Android 8.0+ (minSdk 26). Para probar 16 KB page size, Android 15+.

---

## 1. Variables de entorno de la app (`.env`)

La app lee las claves de un archivo **`.env` en la raíz del repo** (lo consume `app/build.gradle.kts`).

Creá `C:\Users\shinf\Software\TrackItFront\.env` con:

```dotenv
SUPABASE_URL=https://PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=<tu anon key>
ORS_API_KEY=<tu OpenRouteService key>
```

> No commitees el `.env`. La `service_role key` **no** va acá (solo en secrets de Supabase, paso 4).

---

## 2. Base de datos

En el **SQL Editor** del dashboard de Supabase, pegá y ejecutá el contenido completo de:

```
db/setup.sql
```

El script es **idempotente** (`IF NOT EXISTS`, `CREATE OR REPLACE`, `DROP POLICY IF EXISTS`):
seguro de correr aunque ya tengas las tablas base creadas. Solo agrega lo que falte.

El archivo incluye al final los **seeds de prueba** (5 choferes + 70 paquetes). Si no los querés,
borrá o comentá todo lo que está debajo de la sección `SEEDS`.

---

## 3. Desactivar el registro público (signups)

Como el alta es **solo por admin**, desactivá los signups:

- Dashboard → **Authentication → Providers → Email** → desactivá **"Allow new users to sign up"**.
- (Opcional) Confirmación de email según tu política.

---

## 4. Edge Functions — deploy + secrets

Logueate y linkeá el proyecto:

```powershell
supabase login
supabase link --project-ref PROJECT_REF
```

### 4.1 `admin-create-user` (alta de usuarios por admin)

```powershell
supabase functions deploy admin-create-user
supabase secrets set SUPABASE_URL=https://PROJECT_REF.supabase.co
supabase secrets set SUPABASE_ANON_KEY=<anon key>
supabase secrets set SUPABASE_SERVICE_ROLE_KEY=<service_role key>
```

### 4.2 `daily-route-optimizer` (optimización diaria)

```powershell
supabase functions deploy daily-route-optimizer
supabase secrets set ORS_API_KEY=<OpenRouteService key>
# Opcionales (default: Obelisco CABA):
supabase secrets set WAREHOUSE_LAT=-34.6037
supabase secrets set WAREHOUSE_LON=-58.3816
```

> El optimizer ya **resetea los `ASIGNADO` del día** y reasigna (rebalanceo) **sin tocar `CARGADO+`**,
> y escribe `route_order` (orden de visita).

---

## 5. Cron diario (05:00 ART = 08:00 UTC)

Requiere extensiones `pg_cron` y `pg_net` (Dashboard → Database → Extensions).

Editá `supabase/cron/schedule_daily_route_optimizer.sql` reemplazando `PROJECT_REF` y
`SERVICE_ROLE_KEY`, y ejecutalo en el SQL Editor. Si ya existía un schedule con ese nombre:

```sql
select cron.unschedule('daily-route-optimizer');
```

El job hace `POST` a la función con `targetDate = current_date` a las **08:00 UTC** (05:00 Argentina).

---

## 6. Datos de prueba (opcional, dev/testing)

`db/05_seeds.sql` crea **5 choferes**, sus camiones y **~70 paquetes** en todos los estados.
Requiere haber aplicado los pasos `01/03/04`. Es **re-ejecutable**.

- Ejecutá `db/05_seeds.sql` en el SQL Editor.
- Choferes: `chofer1@trackit.test` … `chofer5@trackit.test` — contraseña `password123`.

> Para crear un **admin** (no lo hace el seed): registralo manualmente desde Supabase o,
> si ya tenés un admin, usá *Perfil → Crear usuario* en la app.

Crear un primer admin a mano (SQL Editor), si no tenés ninguno:

```sql
-- 1) Crear el usuario desde Authentication → Add user (email + password, "Auto confirm").
-- 2) Marcarle el rol ADMIN en profiles (reemplazá el email):
update public.profiles
set role = 'ADMIN'
where id = (select id from auth.users where email = 'admin@trackit.test');
```

---

## 7. Compilar y correr la app

Desde la raíz del repo:

```powershell
# Compilar debug
.\gradlew.bat assembleDebug

# Instalar en un dispositivo/emulador conectado
.\gradlew.bat installDebug
```

Verificación de alineación **16 KB page size** (CameraX 1.4.2 / ML Kit 17.3.0 ya alineados):

```powershell
# El APK queda en:
# app\build\outputs\apk\debug\app-debug.apk
```

---

## 8. Prueba funcional sugerida (end-to-end)

1. **Login admin** → *Perfil → Crear usuario*: creá un chofer y un usuario de depósito.
2. **Depósito**: *Ingreso* de varios paquetes (con dirección → geocoding para tener coordenadas).
3. **Chofer**: en *Setup* registrá el camión (patente).
4. **Admin**: *Flota → Generar Rutas del Día* (o esperá el cron). Mirá el snackbar con el resultado.
5. **Admin**: tocá un camión → *Gestionar Ruta*: podés reordenar (subir/bajar) y guardar (`route_order`).
6. **Depósito**: *Cargar camión* → seleccioná paquetes `ASIGNADO`/`EN_DEPOSITO` → confirmá (pasan a `CARGADO`).
7. **Chofer**: *Ruta* (ordenada por `route_order`) y *Mapa* → "Trazar mi ruta". Escaneá para **Entregar**
   (valida que el código coincida con el paquete). Al entregar, se actualiza la ubicación del camión.
8. **Admin**: *Mapa global* → auto-zoom a los camiones + "última vez: …".

---

## 9. Notas / decisiones abiertas

- **`CARGADO` → `EN_CAMINO`**: no hay una acción explícita; el chofer entrega directo desde
  `CARGADO`/`EN_CAMINO`. Si querés un botón "Iniciar recorrido" que marque `EN_CAMINO`, avisá.
- **Depósito marca `CARGADO`** por selección (checkbox) desde la lista; no re-escanea cada paquete.
- **Realtime** no está activo: la propagación entre dispositivos depende del ciclo de `SyncWorker`.
- Revisar **RLS** (carpeta `db/`) antes de producción.
