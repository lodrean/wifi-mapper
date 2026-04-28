# Tasklist: WiFi Heatmap Mapper

## Metadata
- **Ticket ID**: WIFI-MAPPER-001
- **Plan**: docs/plan/WIFI-MAPPER-001.md
- **Status**: In Progress
- **Progress**: 0/10 tasks

## Tasks

### Project Setup
- [ ] Task 1: Create Android project with Gradle
  - **Files**: `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`
  - **Depends**: None
  - **AC**: Project builds successfully, minSdk=26, targetSdk=35, Compose enabled

### Data Layer
- [ ] Task 2: Create Room database and entities
  - **Files**: `data/database/AppDatabase.kt`, `data/database/entity/*.kt`
  - **Depends**: Task 1
  - **AC**: Entities: SessionEntity, MeasurementEntity, TrajectoryPointEntity, AccessPointEntity

- [ ] Task 3: Create repositories
  - **Files**: `data/repository/*RepositoryImpl.kt`
  - **Depends**: Task 2
  - **AC**: WifiScanRepository, SessionRepository, SensorTrackingRepository implemented

### Domain Layer
- [ ] Task 4: Create domain models and use cases
  - **Files**: `domain/model/*.kt`, `domain/repository/*.kt`, `domain/usecase/*.kt`
  - **Depends**: Task 3
  - **AC**: All models, interfaces, and use cases defined

### Presentation Layer
- [ ] Task 5: Create Heatmap Canvas component
  - **Files**: `presentation/map/components/HeatmapCanvas.kt`
  - **Depends**: Task 4
  - **AC**: Canvas draws grid, trajectory, measurement points with RSSI color coding; supports pan/zoom

- [ ] Task 6: Create MapScreen and MapViewModel
  - **Files**: `presentation/map/MapScreen.kt`, `presentation/map/MapViewModel.kt`, etc.
  - **Depends**: Task 5
  - **AC**: MVI pattern (State, Action, Event), real-time session tracking, WiFi scanning integration

- [ ] Task 7: Create Home and Settings screens
  - **Files**: `presentation/home/HomeScreen.kt`, `presentation/settings/SettingsScreen.kt`
  - **Depends**: Task 6
  - **AC**: Session list with CRUD, settings for step calibration, Material 3 UI

### Integration
- [ ] Task 8: Navigation and DI setup
  - **Files**: `di/AppModule.kt`, `presentation/navigation/*.kt`
  - **Depends**: Task 7
  - **AC**: Koin modules registered, NavHost with bottom navigation, type-safe routes

- [ ] Task 9: JSON export/import
  - **Files**: `data/export/JsonMapExporter.kt`, `data/export/JsonMapImporter.kt`
  - **Depends**: Task 8
  - **AC**: Export v1 format, import with validation, share via Intent

- [ ] Task 10: Permissions and foreground service
  - **Files**: `service/TrackingService.kt`, `AndroidManifest.xml`
  - **Depends**: Task 9
  - **AC**: All permissions requested, foreground service for background scanning, persistent notification

## Progress Log
- [2026-04-28 10:15] Tasklist created
