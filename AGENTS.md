# Cinnamon

- Cinnamon is a single-module Android app for contacts, calls, and SMS/MMS messaging.
- Work in the `:app` module; package root and application ID are `com.sosauce.cinnamon`.

## Stack

- Kotlin, Jetpack Compose, Material 3 Expressive, Navigation 3, coroutines/Flow, Koin, DataStore Preferences, Room (KSP), NekoBites.
- Contacts, call logs, SMS/MMS, SIMs and telephony go through Android platform APIs. Room only holds app-owned data (conversation/contact settings, scheduled messages).

## Structure

```text
app/src/main/java/com/sosauce/cinnamon/
  app/          Application startup, Nav3 (`navigation/Screen.kt`, `Navigation.kt`)
  core/         DI, DataStore, shared UI/utils, telephony, services, receivers, workers
  features/     contacts/, messaging/, phone/ (each uses data/domain/presentation)
  settings/     Settings destinations, components, ViewModels
  setup/        Onboarding and default-app/role flows
app/src/main/res/  Strings, drawables and other resources
```

## Commands

- `./gradlew assembleDebug`
- `./gradlew installDebug`
- `./gradlew lintDebug`
- `./gradlew testDebugUnitTest` (no tests yet)
- `./gradlew connectedDebugAndroidTest` (no tests yet)
- Don't run Gradle builds unprompted; ask first.

## Architecture and conventions

- ViewModels own screen state and actions; screens receive state and callbacks.
- Not strict MVI: action/event naming varies. Copy the nearest equivalent screen, don't rename or normalize APIs.
- Nav entries in `app/navigation/Navigation.kt` get ViewModels from Koin, collect state with `collectAsStateWithLifecycle()`, and handle one-off events with NekoBites `ObserveAsEvents`.
- Register dependencies in `core/di/AppModule.kt`; Koin starts in `CinnamonApplication.onKoinStartup()`.
- Examples: `features/contacts/presentation/ContactsViewModel.kt` + `ContactDetailsScreen.kt` (UI/state), `features/contacts/data/repository/ContactsRepository.kt` (ContentResolver).

## Checklists

1. **Add a screen:** Add a `Screen` key in `app/navigation/Screen.kt`; add its `entry<>` in `app/navigation/Navigation.kt`; create UI/state/ViewModel in the feature's `presentation/`; register the ViewModel in `core/di/AppModule.kt`.
2. **Add a setting page:** Add the route in `settings/components/SettingsNavigation.kt` (hosted by `settings/SettingsScreen.kt`); add its ViewModel in `settings/` and register it in `core/di/AppModule.kt`; add text to `strings.xml`.
3. **Add a preference:** Add the key in the existing DataStore setup under `core/datastore/DataStore.kt`; expose read/write through the existing preferences wrapper; never change existing keys.
4. **Add a Room table:** Add entity + DAO in the right database; update `@Database`, bump the version, add a forward migration, and register it in `core/di/AppModule.kt`.
5. **Add a dependency:** Add version + alias to `gradle/libs.versions.toml`; reference the alias from `app/build.gradle.kts`.

## Rules

- Put all user-facing text in `app/src/main/res/values/strings.xml`.
- Use existing Compose/Material 3 Expressive components and theme colors.
- Use existing repository/service abstractions for contacts, telephony, messaging and call logs.
- Never add or upgrade dependencies without asking.
- Never edit generated/build output.
- Never rewrite unrelated code; don't overwrite or revert existing local changes.
- Never use destructive Room migrations; never rename existing database names or DataStore keys.

## Telephony gotchas

- Preserve manifest permissions, roles/default-app flows, intent filters, receivers, services, workers and notifications.
- Never bypass permission, role, SIM/subscription or default-app handling.
- SMS/MMS delivery, scheduled sends and call actions are lifecycle- and permission-sensitive; keep them on the existing coroutine/worker paths.
- Setup flows, backup/import, call-log/voicemail screens and scheduled-message work must keep working when you touch related code.
- Test source dirs may not exist; report accurately which checks you ran.