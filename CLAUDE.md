# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "com.constructiontracker.ExampleUnitTest"

# Clean build
./gradlew clean
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Architecture

Standard Android MVVM with a single-module structure:

**Data layer** (`data/database/`, `data/repository/`):
- Room database (`AppDatabase`) with three entities: `ContractorEntity`, `PaymentEntity`, `PurchaseEntity`
- `ConstructionRepository` is the single source of truth — all ViewModels go through it, never directly to DAOs
- Database is a singleton initialized lazily in `ConstructionTrackerApplication`
- Default contractors (Bathroom & Tile, Electrical, Ceiling) are seeded on first launch in `initializeDefaultContractors()`

**UI layer** (`ui/screens/`, `ui/navigation/`, `ui/theme/`):
- Jetpack Compose with Material3
- Each screen has a paired ViewModel extending `AndroidViewModel` (uses `application as ConstructionTrackerApplication` to get the repository — no DI framework)
- Navigation is bottom-tab based via `AppNavigation.kt`; all 5 routes are defined in the `Screen` sealed class
- ViewModels expose a single `uiState: StateFlow<XxxUiState>` built with `combine()` + `stateIn(WhileSubscribed(5_000))`

**Key domain concepts**:
- `ContractorEntity.contractType` is either `"FIXED"` (has a `contractAmount`) or `"OPEN_ENDED"` (pay-as-you-go)
- `PaymentEntity` links to a contractor via `contractorId`; dates and `createdAt` are stored as Unix epoch millis
- `PurchaseEntity` is for material/supply purchases, independent of contractors
- Currency is formatted as Sri Lankan Rupees (`Rs.`) via `CurrencyUtils.formatCurrency()`

**Tech stack**: Kotlin, Jetpack Compose, Room (with KSP), Navigation Compose, Kotlin Coroutines/Flow. No Hilt/Dagger — dependency wiring is manual through the `Application` class.
