# Implementation Plan: WiFi Heatmap Mapper

## Metadata
- **Ticket ID**: WIFI-MAPPER-001
- **PRD**: docs/prd/WIFI-MAPPER-001.md
- **Research**: docs/research/WIFI-MAPPER-001.md (inline below)
- **Status**: Approved
- **Date**: 2026-04-28

## Architecture Decision

### Approach
Single app module (`:app`) с чётким package-level разделением по Clean Architecture layers. Проект MVP-уровня сложности — multi-module overhead не оправдан. Внутри `:app` пакеты:
- `data` — репозитории,数据源 (WiFi, sensors, DB)
- `domain` — модели, интерфейсы репозиториев, use cases
- `presentation` — MVI экраны, ViewModels, UI models
- `di` — Koin modules

### Alternatives Considered
| Approach | Pros | Cons | Decision |
|----------|------|------|----------|
| Multi-module (:feature:mapping) | Чистая изоляция, быстрая сборка | Сложнее настройка Gradle, оверхед для MVP | Not chosen |
| Single module + package layers | Простота, быстрый старт, легко рефакторить в модули позже | Нет enforced isolation | **Chosen** |

## Component Design

### Data Layer
- Repository: `data/repository/WifiScanRepository` — WiFi scanning через WifiManager
- Repository: `data/repository/SessionRepository` — CRUD сессий через Room
- Repository: `data/repository/SensorTrackingRepository` — датчики перемещения
- Database: `data/database/AppDatabase` — Room с entities: SessionEntity, MeasurementEntity, TrajectoryPointEntity
- JSON: `data/export/JsonMapExporter`, `data/export/JsonMapImporter`

### Domain Layer
- Model: `domain/model/Session`, `domain/model/Measurement`, `domain/model/TrajectoryPoint`, `domain/model/AccessPoint`
- Model: `domain/model/PdrPosition` — позиция от PDR движка
- Repository Interface: `domain/repository/WifiScanRepository`, `domain/repository/SessionRepository`, etc.
- UseCase: `domain/usecase/StartSessionUseCase`, `domain/usecase/ExportSessionUseCase`, etc.

### Presentation Layer
- Screen: `presentation/home/HomeScreen` — список сессий
- Screen: `presentation/map/MapScreen` — основной экран с Canvas картой
- Screen: `presentation/settings/SettingsScreen` — настройки
- ViewModel: `presentation/map/MapViewModel` — управление сессией, сканирование, трекинг
- State: `presentation/map/MapState`
- Action: `presentation/map/MapAction`
- Event: `presentation/map/MapEvent`
- Custom Canvas: `presentation/map/components/HeatmapCanvas` — отрисовка heatmap

## Task Dependencies
```
Task 1 (Project setup)
  → Task 2 (Data layer: Room + Entities)
    → Task 3 (Data layer: Repositories)
      → Task 4 (Domain layer: Models + UseCases)
        → Task 5 (Presentation: Map Canvas + Heatmap)
          → Task 6 (Presentation: MapScreen + ViewModel)
            → Task 7 (Presentation: Home + Settings screens)
              → Task 8 (Navigation + DI)
                → Task 9 (Export/Import JSON)
                  → Task 10 (Permissions + Foreground Service)
```

## Testing Strategy
- Unit tests for: PDR engine (step detection), UseCases, ViewModel
- Integration tests: JSON export/import roundtrip
- Manual QA: Walk test с известным маршрутом

## Risks and Mitigations
| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| WiFi throttling мешает частым измерениям | High | High | Foreground service + инструкция пользователю; адаптивный UI не требует скан чаще ограничений |
| PDR дрейф накапливается быстро | High | High | Калибровка шага в настройках; пользователь может вручную корректировать позицию |
| Canvas heatmap тормозит на больших данных | Med | Med | Grid-агрегация точек; отрисовка только видимой области |
| Эмулятор не имеет реальных датчиков | Med | Low | Fallback на ручной режим (тап по экрану) |
