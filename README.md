# superkassa-core

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/superkassa-core.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=g:io.github.texport)
[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/texport/superkassa-core/releases)
[![Coverage](https://img.shields.io/badge/coverage-98%25-green.svg)](https://github.com/texport/superkassa-core/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![CI Build](https://img.shields.io/github/actions/workflow/status/texport/superkassa-core/ci.yml?branch=main&label=CI%20Build)](https://github.com/texport/superkassa-core/actions)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

## Documentation in English

`superkassa-core` is the core multi-project Kotlin Multiplatform (KMP) library for the **Superkassa** fiscal cash register system. It implements all business rules, data validation, domain entities, and use cases, separated into seven distinct modules:

1. **`core-domain`**: Pure Kotlin Multiplatform domain entities (`Receipt`, `KkmInfo`, `ShiftInfo`) and port definitions (`StoragePort`, `ClockPort`, `DeliveryPort`).
2. **`core-data`**: Implementations of storage backing, OFD communication orchestration, retry policies, and lease locking.
3. **`core-presentation`**: Presentation layer facade (`SuperkassaApi`) that exposes the core system functions to client applications.
4. **`offline-queue`**: Offline database command queue, handling request caching, retry scheduling with backoff policies, state mapping, and error reporting.
5. **`core-string`**: Base localizable string assets, templates, and text resource mappings.
6. **`delivery`**: Transportation network delivery layer for sending documents to remote servers.
7. **`receipt-renderer`**: Print layout engine for building and formatting receipts in HTML and raw configurations, supporting multiple layouts, sizes (58mm, 80mm, Fullscreen), color themes, and multi-language translations (Russian, Kazakh, English).

To use the unified core KMP module in your Multiplatform or JVM Gradle build:

```kotlin
dependencies {
    // For Multiplatform targets
    implementation("io.github.texport:superkassa-core:1.1.0")
    
    // Or for JVM-only targets (like server)
    implementation("io.github.texport:superkassa-core-jvm:1.1.0")
}
```

#### Apple Swift Package Manager

The iOS target is packaged as a unified `SuperkassaCore` binary `XCFramework` distributed via Swift Package Manager. Add the package reference to your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/texport/superkassa-core", from: "1.1.0")
]
```

---

## Документация на русском языке

`superkassa-core` — это основная мультипроектная библиотека Kotlin Multiplatform (KMP) для фискальной системы **Superkassa**. Она реализует все бизнес-правила, валидацию данных, доменные сущности и сценарии использования (Use Cases), разделенные на семь модулей:

1. **`core-domain`**: Чистые сущности предметной области KMP (`Receipt`, `KkmInfo`, `ShiftInfo`) и интерфейсы портов (`StoragePort`, `ClockPort`, `DeliveryPort`).
2. **`core-data`**: Реализации портов хранения, отправки документов в ОФД, политик повторных попыток и межпроцессных блокировок.
3. **`core-presentation`**: Фасад презентационного слоя (`SuperkassaApi`), предоставляющий методы интеграции ядра с внешними клиентами.
4. **`offline-queue`**: Очередь оффлайн-команд в базе данных, кэширование запросов, политики повторных отправлений (backoff) и статусное логирование.
5. **`core-string`**: Базовые локализуемые строковые ресурсы, шаблоны и текстовые маппинги.
6. **`delivery`**: Транспортный сетевой уровень для доставки фискальных документов на удаленные серверы.
7. **`receipt-renderer`**: Движок генерации печатных форм чеков в формате HTML, поддерживающий различные макеты, размеры ленты (58мм, 80мм, Fullscreen), цветовые схемы и многоязыковую локализацию (русский, казахский, английский).

Подключите единый KMP модуль в зависимости вашего Gradle-проекта:

```kotlin
dependencies {
    // Для мультиплатформенных (KMP) проектов
    implementation("io.github.texport:superkassa-core:1.1.0")
    
    // Для классических JVM-проектов (например, сервер)
    implementation("io.github.texport:superkassa-core-jvm:1.1.0")
}
```

#### Apple Swift Package Manager

Для iOS-проектов ядро скомпилировано в бинарный фреймворк `SuperkassaCore.xcframework` и распространяется через Swift Package Manager. Добавьте зависимость в ваш `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/texport/superkassa-core", from: "1.1.0")
]
```

---

## Quick Start / Usage

Here is a quick example of how to initialize and interact with `SuperkassaApi` in your application:

```kotlin
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmInitDirectRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest

// Retrieve the API implementation (e.g., via dependency injection)
val api: SuperkassaApi = ... 

// 1. Initialize a physical KKM (Direct)
val kkm = api.initKkm(
    pin = "1234",
    request = KkmInitDirectRequest(
        ofdId = "kazakhtelecom",
        ofdEnvironment = "prod",
        ofdSystemId = "sys-12345",
        ofdToken = "token-abc-123",
        kkmKgdId = "123456789012",
        factoryNumber = "SWK-0001",
        manufactureYear = 2026
    )
)

// 2. Register a cashier sell receipt
val sellResult = api.createSellReceipt(
    kkmId = kkm.id,
    pin = "1111",
    request = ReceiptSellRequest(
        items = listOf(
            ReceiptItemRequest(
                name = "Фискальный товар",
                price = 1500.0,
                quantity = 1L,
                vatGroup = "VAT_12",
                measureUnitCode = "796"
            )
        ),
        payments = listOf(
            ReceiptPaymentRequest(type = "CASH", sum = 1500.0)
        ),
        idempotencyKey = "unique-receipt-key-1"
    )
)

println("Receipt registered successfully with ticket number: ${sellResult.ticketNumber}")
```

## Architecture Boundary

The project follows a strict Clean Architecture boundary design across all seven modules:

- **core-domain (Entities & Ports):** Pure business logic. All models (`Receipt`, `ShiftInfo`) are fully decoupled from serialization libraries (no `@Serializable` annotations) and have no framework dependencies.
- **core-data (Adapters & Infrastructure):** Implements ports for storage backing, network connections, and database queuing. Uses `internal` adapters to prevent detail leakage.
- **core-presentation (API Facade):** Exposes `SuperkassaApi` and decoupled structures (like `ReceiptSellRequest`, `UserRole`) containing serialization descriptors, preventing leakage of serialization frameworks into the domain layer.
- **offline-queue (Local Database Queue):** Manages local command queueing, retry mechanisms, and localizable error mapping.
- **receipt-renderer (Visual Layout Engine):** Responsible for building print layouts in HTML for different paper widths (58mm, 80mm, Fullscreen), color themes, and multi-language localizations.
- **core-string & delivery:** Utilities for localization strings and transport adapters for delivering documents.

---

### Required Ports for Platform Integration / Обязательные порты для платформенной интеграции

To integrate `superkassa-core` into your host platform (JVM Server, Android App, iOS App), the developer must provide implementation adapters for the following core domain ports:

Для интеграции `superkassa-core` в целевую платформу (JVM Сервер, Android, iOS) разработчик должен предоставить реализации следующих портов:

- **`StoragePort`**: 
  - *EN*: Interface to manage persistence of KKM models, users, active shifts, counters, and document history.
  - *RU*: Интерфейс для сохранения и поиска данных ККМ, пользователей, смен, счетчиков и фискальных документов.
- **`CoreSettingsRepositoryPort`**:
  - *EN*: Interface to load and save system-wide core configurations (`CoreSettings`).
  - *RU*: Интерфейс для чтения и записи системных настроек (`CoreSettings`).
- **`DeliveryPort`**:
  - *EN*: Handles delivering generated receipts/tickets via external communication channels (SMS, Email).
  - *RU*: Обрабатывает отправку чеков через внешние каналы связи (SMS, Email).
- **`ReceiptRenderPort` & `DocumentConvertPort`**:
  - *EN*: Responsible for layout rendering of receipts (e.g. into HTML) and converting them into PDF format.
  - *RU*: Отвечают за визуализацию чеков (например, в HTML) и их конвертацию в PDF.
- **`OfdConnectionPort` / `OfdManagerPort`**:
  - *EN*: Handles establishing physical connections and raw socket payload transfer to and from OFD hosts.
  - *RU*: Отвечают за низкоуровневые TCP/HTTP соединения и обмен пакетами с серверами ОФД.
- **`ClockPort` / `IdGeneratorPort` / `PinHasherPort`**:
  - *EN*: Platform helpers for system time, UUID generation, and secure hashing (PBKDF2/SHA256).
  - *RU*: Системные помощники для времени, генерации UUID и безопасного хеширования ПИН-кодов.

