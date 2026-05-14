# TrackIt

TrackIt is a mocked Android MVP for logistics and package management. It demonstrates role-based navigation, Material 3 UI, and MVVM with in-memory repositories. There is no backend, database, or real map or camera integration in this phase.

## Tech stack

- Kotlin
- Jetpack Compose (Material 3)
- MVVM + repository pattern
- Navigation Compose
- Mocked data via `StateFlow` in repositories

## Requirements

- Android Studio (recommended) with Android SDK installed
- JDK 17 for Gradle and Android builds
- Android device or emulator running API 26+

`local.properties` is generated locally by Android Studio and must not be committed. It points Gradle to your Android SDK.

## Mac compatibility

Yes. This project is cross-platform and works on macOS the same way as on Windows or Linux.

- Open the repository in Android Studio on a MacBook and sync Gradle.
- Run from the IDE on an emulator or a physical Android device.
- From Terminal, use `./gradlew assembleDebug` after making `gradlew` executable: `chmod +x gradlew`.

The Kotlin source, Gradle wrapper, and Android project layout are not Windows-specific. Use JDK 17 on macOS as well; newer JDK versions may break Gradle or the Android Gradle Plugin.

## Getting started

1. Clone the repository.
2. Open the project root in Android Studio.
3. Wait for Gradle sync to finish.
4. Run the `app` configuration on an emulator or device.

### Command line

macOS / Linux:

```bash
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat assembleDebug
```

## Demo logins

Use any non-empty password with one of these emails:

| Email | Role |
| --- | --- |
| `chofer@trackit.com` | Chofer |
| `deposito@trackit.com` | Empleado de depósito |
| `admin@trackit.com` | Administrador |

## Roles and navigation

### Chofer

Bottom navigation: Ruta, Mapa, Perfil.

- Ruta: assigned packages in a list
- Package detail: mocked map, package info, and Escanear to mark delivered
- Mapa: route map placeholder
- Perfil: user info and logout

### Empleado de depósito

Bottom navigation: Ingresos, Historial, Perfil.

- Ingresos: form to register a package into mocked local state
- Historial: recently registered packages
- Perfil: user info and logout

### Administrador

Bottom navigation: Flota, Mapa Global, Perfil.

- Flota: active trucks with delivery progress
- Mapa Global: full-screen map placeholder with daily metrics in a bottom sheet
- Perfil: user info and logout

## Architecture

The app uses a single activity: `MainActivity` hosts `TrackItTheme` and the root `NavHost`. Screens observe ViewModels; ViewModels read and update mocked repositories.

```
UI (Compose) -> ViewModel -> Repository -> mocked StateFlow data
```

## Project structure

```text
app/src/main/java/com/trackit/
  MainActivity.kt
  core/
    navigation/
    ui/theme/
    ui/components/
  data/
    model/
    repository/
  feature/
    auth/
    driver/
    warehouse/
    admin/
    profile/
```

## Scope and limitations

- No REST API, Room, or persistent storage
- Maps and barcode scanning are simulated in the UI
- Login is mocked by email only
- Light and dark themes follow system settings

## License

Academic / demo project unless a license is added by the maintainers.
