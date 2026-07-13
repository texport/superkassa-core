# superkassa-core-domain

[![Version](https://img.shields.io/badge/version-1.0.3-blue.svg)](https://github.com/texport/superkassa-core/releases)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)](https://github.com/texport/superkassa-core/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

## Documentation in English

`superkassa-core-domain` is a pure domain Kotlin Multiplatform (KMP) library for the **Superkassa** fiscal cash register core. It contains no external dependencies, framework integrations, or serialization libraries.

### Key Components
- **Entities**: Domain entities representing fiscal models (`Receipt`, `ShiftInfo`, `KkmInfo`, `TaxRegime`).
- **Ports**: Inversion-of-control interfaces defining system requirements (`StoragePort` for persistence, `ClockPort` for time, and `DeliveryPort` for document delivery).
- **Rules**: Core validation constraints and state transition logic (e.g. `ItemNameRules` and `ShiftRules`).

### Architecture Boundaries
To enforce strict Clean Architecture boundaries, **no serialization framework or annotations** (such as `@Serializable`) are present in this module. Data transformation and API serialization are strictly delegated to the presentation layer.

#### Integration
Add the dependency to your KMP project's `commonMain`:
```kotlin
implementation("io.github.texport:superkassa-core-domain:1.0.3")
```

---

## Документация на русском языке

`superkassa-core-domain` — это чистый доменный Kotlin Multiplatform (KMP) модуль ядра фискальной системы **Superkassa**. Он не содержит внешних зависимостей, интеграций с фреймворками или библиотек сериализации.

### Ключевые компоненты
- **Сущности (Entities)**: Объекты предметной области, представляющие фискальные модели (`Receipt`, `ShiftInfo`, `KkmInfo`, `TaxRegime`).
- **Порты (Ports)**: Интерфейсы инверсии управления, определяющие системные контракты (`StoragePort` для БД, `ClockPort` для времени, `DeliveryPort` для отправки документов).
- **Правила (Rules)**: Бизнес-правила валидации и переходов состояний (например, `ItemNameRules` и `ShiftRules`).

#### Интеграция
Добавьте зависимость в общий набор исходного кода `commonMain` вашего Gradle-проекта:
```kotlin
implementation("io.github.texport:superkassa-core-domain:1.0.3")
```
