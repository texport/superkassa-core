# superkassa-core-presentation

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/superkassa-core-presentation.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.texport/superkassa-core-presentation)
[![Version](https://img.shields.io/badge/version-1.0.3-blue.svg)](https://github.com/texport/superkassa-core/releases)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)](https://github.com/texport/superkassa-core/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI Build](https://img.shields.io/github/actions/workflow/status/texport/superkassa-core/ci.yml?branch=main&label=CI%20Build)](https://github.com/texport/superkassa-core/actions)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

## Documentation in English

`superkassa-core-presentation` is the public API and serialization layer of the **Superkassa** cash register core. It decouples serialization frameworks from the pure domain layer using DTO structures and mappings.

### Key Components
- **API Facade**: Implements the main API interface `SuperkassaApi` and its orchestrations in `SuperkassaApiImpl`.
- **DTOs**: Standardized serialized data transfer objects (e.g. `ReceiptSellRequest`, `UserRoleDto`) marked with `@Serializable` and annotated with OpenAPI metadata.
- **OpenAPI Schema Metadata**: Direct annotations (`Schema`, `Valid`, `Payload`) that are bound via `typealias` to Jakarta validation constraints on JVM/Android platforms.

#### Integration
Add the dependency to your KMP project's `commonMain`:
```kotlin
implementation("io.github.texport:superkassa-core-presentation:1.0.3")
```

---

## Документация на русском языке

`superkassa-core-presentation` — это слой публичного API и сериализации ядра фискальной системы **Superkassa**. Он изолирует доменный слой от библиотек сериализации с помощью DTO-структур и маппингов.

### Ключевые компоненты
- **Фасад API**: Реализует основной интерфейс управления кассой `SuperkassaApi` и его логику в `SuperkassaApiImpl`.
- **DTO**: Сериализуемые структуры данных (например, `ReceiptSellRequest`, `UserRoleDto`), помеченные аннотацией `@Serializable` и дополненные OpenAPI-метаданными.
- **OpenAPI Схемы и Валидация**: Аннотации валидации схем (`Schema`, `Valid`, `Payload`), которые через механизм `typealias` связываются с правилами Jakarta Bean Validation на платформах JVM и Android.

#### Интеграция
Добавьте зависимость в общий набор исходного кода `commonMain` вашего Gradle-проекта:
```kotlin
implementation("io.github.texport:superkassa-core-presentation:1.0.3")
```
