# TrackIt — Plan de finalización (handoff para otro agente)

> **Propósito.** Documento autocontenido para que otro agente (o yo en otra sesión)
> continúe terminando la app sin tener que re-explorar todo. Resume el estado actual,
> las decisiones tomadas, la arquitectura y las **fases pendientes con detalle a nivel de
> archivo**. Leé también el `README.md` (raíz): es la referencia de buenas prácticas y
> patrones (MVVM + repositorio, Compose, UI en español, offline-first con Room como
> única fuente de verdad).

---

## 0. Cómo usar este doc

- Trabajá **una fase a la vez**, en orden (1 → 7). Cada fase tiene: objetivo, estado
  actual real (hallazgos), pasos concretos y criterios de aceptación.
- Después de cada fase, **compilar** con `gradlew.bat assembleDebug` (Windows) y arreglar
  errores antes de seguir.
- Para **cambios de DB**: ya hay `.md` en `db/` con el SQL. Si agregás columnas nuevas,
  creá un nuevo `db/0X_*.md` con explicación + SQL idempotente para correr en Supabase.
- Si hay dudas de producto, **preguntar** antes de asumir.

---

## 1. Decisiones ya tomadas (no volver a preguntar)

- **Fuente de verdad = Room.** Toda lectura de UI sale de Room (Flow). Las escrituras van
  a Room con `pendingSync=true` y se sincronizan a Supabase en background.
- **Optimizer / cron**: corre **05:00 hora Argentina (UTC-3)** y optimiza los paquetes de
  **esa misma fecha** (los paquetes se ingresan el día anterior con `scheduled_date`).
- **Sin capacidad** en el optimizador: reparto parejo de N paquetes entre Y choferes.
- **Usuarios solo por admin**: se elimina el registro público. Ya existe un admin creado
  (no hace falta seed).
- **Re-run del optimizer**: re-optimiza `EN_DEPOSITO` y `ASIGNADO` (no cargados) y
  **excluye `CARGADO` o superior**. Permitir edición manual de la asignación.
- **Ubicación de camiones (admin)**: NO es tiempo real. Se deriva del **último paquete
  entregado + timestamp** (`trucks.last_lat/last_lon/last_location_at`).

---

## 2. Estado actual — qué YA está hecho

### Fase 0 (COMPLETA): base offline-first + el proyecto compila

- **Gradle**: Room, WorkManager y plugin **KSP** agregados (`gradle/libs.versions.toml`,
  `build.gradle.kts` raíz, `app/build.gradle.kts` con `room.schemaLocation`).
- **Capa Room** (`app/src/main/java/com/trackit/data/local/`):
  - `TrackItDatabase` (db `trackit.db`, version 1, `fallbackToDestructiveMigration`).
  - Entities: `PackageEntity`, `TruckEntity`, `ProfileEntity` (con flags `pendingSync`,
    `deletedLocally`, `updatedAtMillis`, `deliveredAtMillis`, etc.).
  - DAOs: `PackageDao`, `TruckDao`, `ProfileDao` (`@Upsert`, `observeAll/observeById`
    Flow, `getPendingSync`, `markSynced`, `deleteById`).
  - `mapper/EntityMappers.kt`: `toDomain()` / `toEntity(...)` entre entity ↔ dominio.
- **Modelos** (`data/model/`):
  - `Package` ahora tiene `barcode`, `routeOrder`, `updatedAt: Instant?`,
    `deliveredAt: Instant?` (todos con `@SerialName` snake_case).
  - `Truck` tiene `lastLocationAt: Instant?`. `deliveredCount`/`totalCount` están marcados
    **`@Transient`** (se calculan en cliente; NO son columnas — si los serializás, el
    upsert a `trucks` rompe).
  - `RouteOptimizationResult` (resultado de dominio del optimizer).
  - `serialization/InstantSerializer.kt` (timestamptz ↔ Instant, tolera `Z` y `+00:00`).
- **Repos**: se eliminaron `SupabasePackageRepository`/`SupabaseFleetRepository`
  (duplicaban caché). Quedan **`OfflineFirstPackageRepository`** y
  **`OfflineFirstFleetRepository`** como únicos repos de datos. `SupabaseAuthRepository`
  sigue para auth.
- **Service locator** (`SupabaseLocator`): expone `client`, `authRepository`,
  `packageRepository`, `fleetRepository`, `networkObserver` (singletons). Se inicializa en
  `TrackItApp.onCreate()` con `SupabaseLocator.init(this, supabase)`, registra el observer
  de red y dispara un primer sync.
- **ViewModels**: los 13 usan `SupabaseLocator.*` (esto además arregla el bug de
  `currentUser` que quedaba nulo entre pantallas).
- **`invokeRouteOptimizer`** (`data/repository/RouteOptimizerFunction.kt`): extensión que
  llama al Edge Function y parsea `{ok,targetDate,assigned,unassigned,reason,error}`.

### `/sync` revisado y CORREGIDO

`SyncWorker` (push pending → pull remote), `SyncScheduler` (WorkManager, único `enqueue`),
`NetworkConnectivityObserver` (sync al recuperar red). Fixes aplicados:
- `Truck.deliveredCount/totalCount` → `@Transient` (antes el upsert de trucks fallaba
  siempre).
- Pull **no pisa filas con `pendingSync=true`** (antes podía perder ediciones offline
  hechas durante la ventana de sync).
- `Result.retry()` ahora con tope (`MAX_RETRIES=3`) para no loopear ante errores de schema.
- `OfflineFirstPackageRepository.triggerRouteOptimization` ahora llama de verdad a
  `invokeRouteOptimizer` (antes devolvía `NotImplementedError`).
- `OfflineFirstPackageRepository.updateStatus` vuelve a limpiar `assignedDriverId` cuando
  pasa a `EN_DEPOSITO` y setea `deliveredAt` cuando pasa a `ENTREGADO`.

### Entregables de DB (en `db/`)

- `db/01_base_schema.md` — `profiles`, `trucks`, `packages` + RLS base.
- `db/02_admin_only_users.md` — `is_admin()`, RLS solo-admin, **deshabilitar signups**.
- `db/03_offline_and_tracking.md` — `updated_at`, `delivered_at`, `last_location_at` +
  trigger de `updated_at`.
- `db/04_barcode_and_route_order.md` — `barcode`, `route_order`.
- `db/05_seeds.sql` — **datos de prueba** (dev/testing): 5 choferes en `auth.users`
  (`chofer1..5@trackit.test` / `password123`) + perfiles + camiones + 70 paquetes en
  todos los estados, con coords variadas de CABA. Re-ejecutable. Requiere `01/03/04`.

**⚠️ Antes de correr la app**: aplicar al menos `db/03` y `db/04` en Supabase (sino los
upserts/selects fallan por columnas inexistentes). `db/02` cuando se active admin-only.

---

## 3. Arquitectura y "gotchas" para el nuevo agente

- **Lectura**: `repo.packages` / `repo.trucks` son `StateFlow` alimentados por
  `dao.observeAll()` (Room). La UI nunca lee de red directo.
- **Escritura**: `dao.upsert(entity.copy(pendingSync=true))` + `SyncScheduler.enqueue(context)`.
- **Sync**: WorkManager (constraint: red). Push primero (upsert/delete a PostgREST,
  `markSynced`), luego pull (sin pisar pendientes).
- **Conflictos**: last-write-wins simple (las filas pendientes locales ganan hasta que se
  pushean). El server mantiene `updated_at` via trigger.
- **Gotchas**:
  - No serializar campos que no son columnas (usar `@Transient`), o el upsert rompe.
  - `id` de packages/trucks se generan en cliente con `UUID.randomUUID()`.
  - `OfflineFirstFleetRepository.createTruck` **no** deduplica por chofer (puede crear
    duplicados) y no normaliza la patente — mejorar al tocar fleet.
  - `getDriverPackages` hoy **no** ordena por `routeOrder` (ver Fase 4/5).

---

## 4. Fases pendientes (detalle)

### FASE 1 — Usuarios solo creados por admin — ✅ APP HECHA (falta deploy + DB)

**Hecho en la app (sesión actual):**
- `IAuthRepository`: se quitó `register(...)` y se agregó
  `createUserAsAdmin(email, password, displayName, role): Result<Unit>`.
- `SupabaseAuthRepository.createUserAsAdmin`: llama `functions.invoke("admin-create-user", ...)`
  (reenvía el JWT del admin, NO cambia su sesión); parsea `{ok,error}` y mapea errores a
  mensajes en español. `resolveUserFromSession()` ahora hace `awaitInitialization()`
  (con timeout de 5s) para reconocer la sesión persistida.
- Registro público eliminado: borrados `RegisterScreen` + `RegisterViewModel`, quitada la ruta
  `REGISTER` de `Routes.kt` y del `NavGraph`, y el link en `LoginScreen`.
- Nuevo `feature/admin/createuser/CreateUserScreen.kt` + `CreateUserViewModel` (nombre, email,
  password, rol; TopAppBar con back; snackbar de éxito).
- `Routes.ADMIN_CREATE_USER` + composable en `AdminNavGraph`; `ProfileScreen` recibe
  `onCreateUser` opcional (botón "Crear usuario", solo cableado en el perfil admin).
- **Sesión persistente**: nuevo `Routes.SPLASH` (start destination) con `SplashScreen` que
  resuelve la sesión y navega directo al home del rol (o a login si no hay sesión).

**Pendiente (fuera de la app, no se pudo hacer desde acá):**
- `supabase functions deploy admin-create-user` + secrets `SUPABASE_URL`,
  `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_ANON_KEY`.
- Aplicar `db/02_admin_only_users.md` y **desactivar signups** en el dashboard de Supabase.
- ⚠️ **Compilación no verificada**: la terminal del entorno dejó de responder; falta correr
  `gradlew.bat assembleDebug`.

**Estado actual (hallazgos originales):**
- Registro público **sigue activo y accesible**: `LoginScreen` tiene link
  `onNavigateToRegister`; ruta `Routes.REGISTER`; `RegisterScreen` + `RegisterViewModel`
  con selector de rol (incluye ADMIN); `SupabaseAuthRepository.register()` hace
  `signUpWith` + insert en `profiles`.
- No existe `CreateUserScreen` ni el Edge Function `admin-create-user` (estaban en un
  índice viejo pero NO en disco).
- `IAuthRepository` expone `register(...)`. No hay `createUserAsAdmin`.
- `ProfileScreen` solo tiene logout. `AdminNavGraph` no tiene ruta de crear usuario.
- No hay sesión persistente: el arranque siempre cae en `Routes.LOGIN` (no se llama a
  `resolveUserFromSession()` al inicio).

**Pasos:**
1. **Edge Function** `supabase/functions/admin-create-user/index.ts` — ✅ **YA CREADO**.
   - Hace: POST; valida `Authorization: Bearer <jwt>`; verifica `profiles.role=='ADMIN'`
     (service role); `auth.admin.createUser(... email_confirm:true, user_metadata)`;
     insert en `profiles` con rollback si falla; devuelve `{ ok, user }` o error tipado
     (`missing_auth`/`forbidden_not_admin`/`invalid_role`/etc.).
   - **Falta**: `supabase functions deploy admin-create-user` + setear secrets
     `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_ANON_KEY`.
2. **App**:
   - `IAuthRepository`: agregar `suspend fun createUserAsAdmin(email, password,
     displayName, role): Result<Unit>`; quitar `register(...)`.
   - `SupabaseAuthRepository`: implementar `createUserAsAdmin` con
     `supabase.functions.invoke("admin-create-user", body=...)` (el SDK reenvía el JWT del
     admin logueado). NO debe cambiar la sesión del admin.
   - Borrar `RegisterScreen` + `RegisterViewModel`; quitar ruta `REGISTER` de `Routes.kt`
     y del `NavGraph`; quitar el link en `LoginScreen`.
   - Crear `feature/admin/createuser/CreateUserScreen.kt` + `CreateUserViewModel`
     (campos: nombre, email, password, rol Chofer/Depósito/Admin). Mensajes en español.
   - Agregar ruta `Routes.ADMIN_CREATE_USER` en `AdminNavGraph`; botón "Crear usuario" en
     el perfil del admin (`ProfileScreen` recibe un `onCreateUser` opcional, o variante
     admin).
   - **Sesión persistente**: en `NavGraph`/arranque, llamar `resolveUserFromSession()` y
     si hay sesión, navegar directo al home del rol (saltear login).
3. **DB / dashboard**: aplicar `db/02_admin_only_users.md` y **desactivar signups** en
   Supabase Auth.

**Aceptación:** no hay forma de registrarse desde el login; un admin puede crear usuarios
de cualquier rol; un no-admin que intente el Edge Function recibe 403; reabrir la app con
sesión válida no pide login de nuevo.

---

### FASE 2 — Completar offline/sync

Gran parte hecha (ver §2). Pendiente/mejoras:
1. **`getDriverPackages`**: ordenar por `routeOrder` (asc, nulls al final) y luego `eta`.
2. **`createTruck`**: deduplicar por `driverId` (si ya existe, devolverlo) y normalizar
   patente (`trim().uppercase()`).
3. **Realtime (opcional, recomendado)**: suscribirse a `packages`/`trucks` y, ante un
   cambio remoto, encolar sync (o escribir en Room) para que admin/chofer vean cambios sin
   reabrir pantalla. Habilitar replicación en Supabase.
4. **Errores de red en UI**: mensajes claros en español cuando una acción quede solo local
   (encolada) vs error real.

**Aceptación:** marcar entregado/cargado y crear paquetes funciona sin red y sincroniza al
volver; el chofer ve su ruta en orden de `routeOrder`.

---

### FASE 3 — CameraX + escaneo correcto

**Estado actual (hallazgos):**
- `core/ui/components/BarcodeScannerSheet.kt`: CameraX + ML Kit funcionan, con campo de
  ingreso manual. PERO: el permiso `CAMERA` está **hardcodeado** (`hasCameraPermission =
  true`) y **no está declarado en el manifest**. Sin debounce de escaneo (dispara muchas
  veces). El `BarcodeScanner` de ML Kit no se cierra. Overlay con tamaño mal (140 vs 180dp).
- Se usa en: Intake (gate antes de submit), RouteScreen "Entregar", PackageDetail
  "Entregar". En entrega, **el código escaneado se ignora** (entrega por `id` ya
  seleccionado). El barcode del intake **ya se persiste** (fix de Fase 0).
- Estados `EN_CAMINO` y `FALLIDO`: nunca se setean en la app. `CARGADO` se setea en
  `LoadTruckViewModel` **sin escaneo**.

**Pasos:**
1. Manifest: agregar `<uses-permission android:name="android.permission.CAMERA"/>` y
   `<uses-feature android:name="android.hardware.camera.any" android:required="false"/>`.
2. `BarcodeScannerSheet`: permiso runtime real (`rememberLauncherForActivityResult` +
   `ContextCompat.checkSelfPermission`), rationale en español; debounce "una sola
   detección"; cerrar el scanner ML Kit; arreglar overlay.
3. **Validación de ID en cambios de estado manuales** (requisito clave): al
   cargar/entregar, comparar el código escaneado/manual contra `pkg.barcode` (o `pkg.id`
   si barcode vacío). Si no coincide, error y no cambiar estado.
   - Driver `PackageDetailViewModel.onCodeScanned(code)`: usar `code` para validar.
   - `RouteScreen` "Entregar": idem.
4. **CARGADO con escaneo**: en depósito/chofer, permitir marcar `CARGADO` escaneando o
   ingresando el ID (hoy `LoadTruck` lo hace por checkbox sin validar).
5. Exponer transición `CARGADO → EN_CAMINO` donde corresponda (o automatizar al primer
   movimiento del chofer — definir con el usuario).

**Aceptación:** no se puede ingresar un paquete sin ID (escaneo o manual); cambiar estado
manualmente requiere el ID correcto del paquete; la cámara pide permiso correctamente.

---

### FASE 4 — Script de asignación (optimizer)

**Estado actual (hallazgos):** `supabase/functions/daily-route-optimizer/index.ts`:
- POST con `{targetDate?}` (default hoy). N = `packages` con `status='EN_DEPOSITO'` +
  `scheduled_date=targetDate` + coords no nulas. Y = `trucks` con `driver_id` no nulo.
- Llama ORS `/optimization` (VROOM), una vehicle por camión, un job por paquete.
- Aplica `status='ASIGNADO'` + `assigned_driver_id`. **No** escribe `route_order` ni `eta`.
- Re-run: solo toca `EN_DEPOSITO` → respeta `CARGADO+` pero **también saltea `ASIGNADO`**
  (no rebalancea los asignados no cargados).
- Cron `supabase/cron/schedule_daily_route_optimizer.sql`: `0 2 * * *` (UTC), target
  **mañana**. El botón admin (`FleetViewModel.runDailyCronJob`) usa `LocalDate.now()` (hoy)
  → **desalineado** con el cron.
- `LoadTruckViewModel` solo lista `EN_DEPOSITO` → tras asignar, el depósito **no ve** los
  `ASIGNADO` para marcarlos `CARGADO` (handoff roto).
- `AssignRouteScreen`: permite agregar/quitar paquetes a un chofer; sin reordenar; sin
  filtrar por fecha; permite desasignar `CARGADO`/`EN_CAMINO` (riesgoso).

**Pasos:**
1. **Edge Function**:
   - Antes de optimizar, **resetear** los `ASIGNADO` del `targetDate` a `EN_DEPOSITO`
     (para rebalancear) — **sin** tocar `CARGADO` o superior.
   - Escribir `route_order` por paquete según el orden de visita de cada `route.steps`.
   - Devolver counts (ya lo hace). Mantener `unassigned`.
2. **Cron**: cambiar a `0 8 * * *` (08:00 UTC = 05:00 ART) y `targetDate = current_date`
   (ese mismo día). Reemplazar `PROJECT_REF`/`SERVICE_ROLE_KEY`. Documentar en un nuevo
   `db/0X_cron.md` si se modifica.
3. **App**:
   - `FleetViewModel.runDailyCronJob`: usar la **misma fecha** que el cron (hoy ART) y
     mostrar el `RouteOptimizationResult` (assigned/unassigned/reason) en la UI.
   - `LoadTruckViewModel`: listar también `ASIGNADO` (no solo `EN_DEPOSITO`) para poder
     cargar.
   - `AssignRouteScreen`: agregar reordenar (drag o subir/bajar) que escriba `route_order`;
     no permitir desasignar paquetes `CARGADO+`; filtrar disponibles por `scheduled_date`.

**Aceptación:** a las 05:00 ART se asignan los paquetes del día a los choferes con orden de
ruta; re-correr no toca `CARGADO+` pero rebalancea el resto; el admin puede editar la
asignación manualmente; el depósito puede cargar los asignados.

---

### FASE 5 — Mapas

**Estado actual (hallazgos):**
- `feature/map/MapScreen.kt` (usado por `DriverMapScreen`): muestra GPS (osmdroid
  `MyLocationNewOverlay`) y markers de paquetes asignados, pero la **polyline ORS solo se
  dibuja tras buscar una dirección manualmente** (no auto-rutea a los paquetes). Además la
  ruta `driver/map` **no está en el bottom nav** del chofer (inalcanzable).
- `feature/admin/globalmap`: dibuja markers desde `truck.last_lat/last_lon` (sin auto-zoom,
  sin timestamp). **Nada llama a `updateTruckLocation`**, así que los camiones no aparecen.
- `MapRepository`: Photon (geocoding) + ORS `/directions`. `updateTruckLocation` existe en
  `OfflineFirstFleetRepository` (fix Fase 0) pero **nadie la llama todavía**.

**Pasos:**
1. **Chofer**: al abrir el mapa, calcular y dibujar la ruta desde su ubicación a los
   destinos asignados (en orden `routeOrder`). Exponer el mapa en la navegación del chofer
   (bottom nav o botón desde la ruta).
2. **Admin**: al marcar un paquete `ENTREGADO`, llamar `fleetRepository.updateTruckLocation`
   con las coords del paquete entregado + ahora (`last_location_at`). Mostrar en el marker
   "última vez: <hora>".
   - Dónde: en el flujo de entrega (driver) tras `updateStatus(ENTREGADO)`. Buscar el
     truck del chofer (`getTruckForDriver`) y actualizar.
3. `GlobalMap`: auto-zoom a los markers; mostrar timestamp (`lastLocationAt`).

**Aceptación:** el chofer ve el camino hacia sus paquetes; el admin ve dónde estuvo cada
camión y a qué hora (derivado de la última entrega).

---

### FASE 6 — UX / navegación

**Estado actual (hallazgos):**
- Sin botón atrás (TopAppBar) en **Intake** (`warehouse/intake`) y **LoadTruck paso 1**
  (`warehouse/load_truck`). `WarehouseNavGraph` no les pasa `onBack`/navController.
- Placeholders hardcodeados en `PackageDetailScreen` ("Peso 4,5 kg", "40x30x20 cm").

**Pasos:**
1. Agregar `TopAppBar` con `navigationIcon` (ArrowBack → `navController.navigateUp()`) en
   Intake y LoadTruck (paso 1). Pasar `onBack` desde `WarehouseNavGraph`.
2. Revisar mensajes de error/carga en español en todas las pantallas.
3. Quitar/realizar con datos reales los placeholders de `PackageDetailScreen`.

**Aceptación:** todas las sub-pantallas del depósito tienen forma visible de volver.

---

### FASE 7 — Revisión integral

1. Código muerto: `MapPlaceholder`, APIs ORS sin usar en cliente
   (`OpenRouteServiceOptimizationApi`), estado `FALLIDO` sin flujo (definir si se usa).
2. Seguridad: confirmar RLS (`db/01`,`db/02`), service role solo en Edge Functions, anon
   key en app.
3. Actualizar `README.md` al modelo admin-only + offline (hoy describe registro público).
4. Opcional: correr Bugbot / Security review.

---

## 5. Bugs / regresiones conocidas abiertas (además de las fases)

- `OfflineFirstFleetRepository.createTruck`: sin dedup por chofer ni normalización de patente.
- `getDriverPackages`: no ordena por `routeOrder`.
- `FleetViewModel.runDailyCronJob`: usa `LocalDate.now()` y no muestra el resultado real.
- Registro público sigue presente (Fase 1).
- `EN_CAMINO`/`FALLIDO` nunca se setean (Fase 3/4).
- `LoadTruck` no ve `ASIGNADO` (Fase 4).
- Mapas: sin auto-ruta chofer ni ubicación de camión (Fase 5).
- Back buttons depósito (Fase 6).

---

## 6. Verificación

```bash
# Windows
gradlew.bat assembleDebug
```

- Aplicar SQL de `db/03` y `db/04` en Supabase **antes** de probar en runtime.
- Para admin-only: `db/02` + desactivar signups en el dashboard.
- Probar: login → según rol; ingreso de paquete con código; entrega offline y sync al
  volver la red; "Generar rutas del día" (admin).
```
