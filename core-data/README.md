# superkassa-core-data

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/superkassa-core-data.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.texport/superkassa-core-data)
[![Version](https://img.shields.io/badge/version-1.0.3-blue.svg)](https://github.com/texport/superkassa-core/releases)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)](https://github.com/texport/superkassa-core/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI Build](https://img.shields.io/github/actions/workflow/status/texport/superkassa-core/ci.yml?branch=main&label=CI%20Build)](https://github.com/texport/superkassa-core/actions)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

## Documentation in English

`superkassa-core-data` is the data and infrastructure adapter layer for the **Superkassa** cash register core. It implements the interfaces defined in the domain layer.

### Key Components
- **OFD Integration**: Orchestrates communication with the fiscal data operator (OFD) via `OfdManagerAdapter`.
- **Generator Adapters**: Implements `UuidGeneratorAdapter` for transaction tracing.
- **Queue Synchronization**: Bridges database operations with the `offline-queue` library.

#### Integration
Add the dependency to your KMP project's `commonMain`:
```kotlin
implementation("io.github.texport:superkassa-core-data:1.0.3")
```

---

## Документация на русском языке

`superkassa-core-data` — это слой данных и инфраструктурных адаптеров ядра фискальной системы **Superkassa**. Он реализует порты интерфейсов, определенные на доменном уровне.

### Ключевые компоненты
- **Интеграция с ОФД**: Реализует логику сетевого обмена с оператором фискальных данных через `OfdManagerAdapter`.
- **Генераторы идентификаторов**: Реализует `UuidGeneratorAdapter` для отслеживания транзакций чеков.
- **Очередь офлайн-синхронизации**: Обеспечивает связывание операций хранения с библиотекой `offline-queue`.

#### Интеграция
Добавьте зависимость в общий набор исходного кода `commonMain` вашего Gradle-проекта:
```kotlin
implementation("io.github.texport:superkassa-core-data:1.0.3")
```
