# PRD: WiFi Heatmap Mapper

## Metadata
- **Ticket ID**: WIFI-MAPPER-001
- **Status**: Approved
- **Author**: Analyst (AI)
- **Date**: 2026-04-28

## Overview
Мобильное приложение для Android, которое строит карту покрытия WiFi-сети в помещении с помощью пешеходной навигации без GPS (PDR — Pedestrian Dead Reckoning) и периодического сканирования WiFi. Пользователь обходит помещение, приложение отслеживает перемещение через датчики смартфона, фиксирует RSSI в каждой точке и визуализирует heatmap на Canvas.

## Goals
- Построить точную карту покрытия WiFi в формате heatmap в реальном времени
- Обеспечить автономную работу без необходимости загрузки планов помещений
- Предоставить красивый UI в стиле Material Design 3
- Обеспечить обмен данными через открытый JSON-формат

## User Stories
- As a user, I want to start a new mapping session so that I can survey WiFi coverage in my area
- As a user, I want to see my movement trajectory in real time so that I know where I've already been
- As a user, I want to see a heatmap of WiFi signal strength so that I can identify weak spots
- As a user, I want to export my map as JSON so that I can share it with others or use in other apps
- As a user, I want to import a previously saved map so that I can continue working with it
- As a user, I want to calibrate my step length so that the tracking is more accurate

## Acceptance Criteria
- [ ] AC1: Приложение корректно запрашивает все runtime permissions (Location, Nearby Devices на Android 12+)
- [ ] AC2: WiFi scan выполняется с интервалом не чаще ограничений OS (throttling-aware), foreground сервис при необходимости
- [ ] AC3: PDR движок детектирует шаги с точностью ≥ 95% на ровной поверхности
- [ ] AC4: Траектория отображается в реальном времени на Canvas с масштабированием и панорамированием
- [ ] AC5: Heatmap интерполирует RSSI между точками измерений (inverse distance weighting или подобный алгоритм)
- [ ] AC6: Поддерживается экспорт и импорт JSON формата v1 согласно спецификации
- [ ] AC7: UI полностью следует Material 3 (color scheme, typography, shapes, components)
- [ ] AC8: Приложение работает на Android 8.0+ (API 26+)

## Edge Cases
| Case | Handling |
|------|----------|
| WiFi throttling (Android 9+) | Показывать уведомление пользователю с инструкцией отключить throttle в Developer Options; использовать foreground service для максимальной частоты сканирования |
| Датчики недоступны (эмулятор) | Fallback на ручной режим: пользователь тапает на экране для перемещения и фиксации точки измерения |
| Нет WiFi сетей в радиусе | Показывать empty state с предложением проверить настройки WiFi |
| Пользователь сворачивает приложение | Foreground service продолжает сканирование и трекинг; показывать persistent notification |
| Резкий поворот/наклон телефона | PDR фильтрует выбросы через комплементарный фильтр; отбрасывать шаги при слишком большом угле наклона |
| Очень большая карта (>1000 точек) | Использовать quadtree или grid-агрегацию для heatmap; ленивая загрузка точек |
| Низкий заряд батареи | Предупреждать пользователя; предлагать остановить сессию |

## Technical Constraints
- **Min SDK**: API 26 (Android 8.0)
- **Target SDK**: API 35 (Android 15)
- **WiFi scanning**: WifiManager.startScan() с ограничениями Android 9+ (4 скана за 2 минуты foreground)
- **Permissions**: `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS` (Android 13+)
- **Sensors**: `TYPE_ACCELEROMETER`, `TYPE_GYROSCOPE`, `TYPE_MAGNETIC_FIELD`, `TYPE_STEP_DETECTOR` (optional fallback)
- **Architecture**: Clean Architecture + MVI presentation pattern
- **Database**: Room для хранения сессий и измерений
- **DI**: Koin
- **UI**: Jetpack Compose Material 3
- **Serialization**: KotlinX Serialization для JSON экспорта/импорта

## UI/UX
- **Screens affected**: 
  - Home / Projects list
  - Map session (основной экран с Canvas картой)
  - Session review (просмотр завершённой сессии)
  - Settings (калибровка шага, выбор единиц)
  - Export / Import
- **Navigation flow**: Bottom navigation bar с 3 вкладками: Maps (список/карта), New Session, Settings
- **Error states**: Snackbar с UiText для ошибок; Dialog для критических ошибок permissions
- **Loading states**: CircularProgressIndicator при инициализации сенсоров; skeleton для списка карт

## Dependencies
- `androidx.compose.material3:material3` — Material 3 components
- `androidx.navigation:navigation-compose` — Type-safe navigation
- `io.insert-koin:koin-androidx-compose` — DI
- `androidx.room:room-runtime` + ksp — Database
- `org.jetbrains.kotlinx:kotlinx-serialization-json` — JSON
- `androidx.lifecycle:lifecycle-viewmodel-compose` — ViewModel
- `androidx.core:core-ktx` — Core extensions

## Metrics
- Точность детекции шагов (steps detected / actual steps)
- Среднее отклонение траектории от реального пути (при тестировании)
- Время между WiFi сканами (учитывая throttling)
- Количество точек измерений в сессии
- Размер экспортируемого JSON файла

## Open Questions
- Нужна ли интеграция с облаком для бэкапа карт? (не в MVP)
- Нужна ли поддержка нескольких этажей (z-coordinate)? (не в MVP)

## Appendix

### JSON Export Format v1
```json
{
  "version": 1,
  "name": "Home WiFi Map",
  "createdAt": "2026-04-28T10:00:00Z",
  "updatedAt": "2026-04-28T11:30:00Z",
  "stepLengthMeters": 0.75,
  "accessPoints": [
    {
      "bssid": "aa:bb:cc:dd:ee:ff",
      "ssid": "MyWiFi",
      "frequencyMHz": 2412,
      "standard": "802.11n"
    }
  ],
  "measurements": [
    {
      "id": "uuid",
      "x": 0.5,
      "y": 1.2,
      "rssiDbm": -45,
      "bssid": "aa:bb:cc:dd:ee:ff",
      "timestamp": 1714293600000
    }
  ],
  "trajectory": [
    {
      "x": 0.0,
      "y": 0.0,
      "headingDegrees": 45.0,
      "timestamp": 1714293599000,
      "isStep": true
    }
  ]
}
```

### WiFi RSSI Color Scale
| RSSI (dBm) | Color | Quality |
|------------|-------|---------|
 ≥ -50 | Green (#4CAF50) | Excellent |
 -51 to -60 | Light Green (#8BC34A) | Good |
 -61 to -70 | Yellow (#FFEB3B) | Fair |
 -71 to -80 | Orange (#FF9800) | Poor |
 ≤ -81 | Red (#F44336) | Very Poor |
