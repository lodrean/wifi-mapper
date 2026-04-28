# WiFi Mapper

Android-приложение для построения карт покрытия WiFi-сети в помещениях с помощью пешеходной навигации без GPS (PDR — Pedestrian Dead Reckoning).

## Возможности

- **Автономное картографирование** — приложение строит карту само, без загрузки планов помещений
- **PDR-трекинг** — отслеживание перемещения через акселерометр, гироскоп и магнитометр
- **WiFi-сканирование** — измерение RSSI (мощности сигнала) в dBm
- **Heatmap-визуализация** — цветовая карта покрытия с масштабированием и панорамированием
- **Material Design 3** — современный адаптивный интерфейс
- **Экспорт/импорт JSON** — открытый формат для обмена картами между приложениями

## Архитектура

- **Clean Architecture** — разделение на Data, Domain и Presentation слои
- **MVI** — State, Action, Event паттерн для UI
- **Koin** — dependency injection
- **Room** — локальное хранилище сессий и измерений
- **Jetpack Compose + Material 3** — декларативный UI

## Требования

- Android 8.0+ (API 26+)
- Датчики: акселерометр, гироскоп (опционально), магнитометр (опционально)
- Разрешения: Location, WiFi State, Foreground Service

## Сборка

```bash
./gradlew :app:assembleDebug
```

## JSON Export Format v1

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

## WiFi Scanning Throttling

На Android 9+ (API 28+) система ограничивает частоту WiFi-сканирования:
- Foreground app: максимум 4 скана за 2 минуты
- Background: 1 скан за 30 минут

Для точных измерений рекомендуется отключить throttling в Developer Options:
**Settings → Developer Options → Wi-Fi scan throttling → OFF**

## PDR Калибровка

Точность трекинга зависит от длины шага пользователя. Откалибруйте значение в Settings:
- Средний шаг: 0.70–0.80 м
- Малый рост: 0.55–0.65 м
- Высокий рост: 0.80–0.95 м

## Известные ограничения

- PDR дрейфует со временем (накапливающаяся погрешность)
- WiFi throttling на Android 9+ ограничивает плотность измерений
- На эмуляторе датчики недоступны — используйте ручной режим
