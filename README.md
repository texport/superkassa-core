# Модуль :superkassa-core

## Назначение
Этот модуль является ядром (Domain + Application) системы ККМ Superkassa. Он содержит ключевые бизнес-правила и логику работы кассового аппарата, полностью изолированные от внешних зависимостей (Spring, Android SDK, базы данных и т.д.).

## Содержимое
- **Слой Domain**: Бизнес-сущности (`Receipt`, `KkmInfo`, `ShiftInfo`, `Money`), типы данных и интерфейсы портов (`StoragePort`, `ClockPort`, `DeliveryPort`, `TimeValidatorPort`, `QrCodeGeneratorPort`).
- **Слой Application (Use Cases)**: Сервисы управления жизненным циклом ККМ, проведения продаж, управления сменами, номенклатуры и синхронизации с ОФД.
- **Данные и рендеринг**: XHTML-шаблоны чеков и рендеринг в двуязычный HTML (`ReceiptHtmlRenderer`).

## Архитектурные ограничения
- Совместим с Java 17 и Android SDK.
- Запрещено импортировать классы из слоев `presentation`, `storage-jdbc` и `server`.
- Зависит только от `kotlinx.serialization` и `kotlinx.coroutines`.
