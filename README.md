# superkassa-core

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/superkassa-core.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=g:io.github.texport)
[![Version](https://img.shields.io/badge/version-1.0.3-blue.svg)](https://github.com/texport/superkassa-core/releases)
[![Coverage](https://img.shields.io/badge/coverage-91%25-green.svg)](https://github.com/texport/superkassa-core/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![CI Build](https://img.shields.io/github/actions/workflow/status/texport/superkassa-core/ci.yml?branch=main&label=CI%20Build)](https://github.com/texport/superkassa-core/actions)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

## Documentation in English

`superkassa-core` is the core multi-project Kotlin Multiplatform (KMP) library for the **Superkassa** fiscal cash register system. It implements all business rules, data validation, domain entities, and use cases, separated into three distinct modules:

1. **`core-domain`**: Pure Kotlin Multiplatform domain entities (`Receipt`, `KkmInfo`, `ShiftInfo`) and port definitions (`StoragePort`, `ClockPort`, `DeliveryPort`).
2. **`core-data`**: Implementations of storage backing, OFD communication orchestration, retry policies, and lease locking.
3. **`core-presentation`**: Presentation layer facade (`SuperkassaApi`) that exposes the core system functions to client applications.

To use the unified core KMP module in your Multiplatform or JVM Gradle build:

```kotlin
dependencies {
    // For Multiplatform targets
    implementation("io.github.texport:superkassa-core:1.0.3")
    
    // Or for JVM-only targets (like server)
    implementation("io.github.texport:superkassa-core-jvm:1.0.3")
}
```

#### Apple Swift Package Manager

The iOS target is packaged as a unified `SuperkassaCore` binary `XCFramework` distributed via Swift Package Manager. Add the package reference to your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/texport/superkassa-core", from: "1.0.3")
]
```

---

## Документация на русском языке

`superkassa-core` — это основная мультипроектная библиотека Kotlin Multiplatform (KMP) для фискальной системы **Superkassa**. Она реализует все бизнес-правила, валидацию данных, доменные сущности и сценарии использования (Use Cases), разделенные на три модуля:

1. **`core-domain`**: Чистые сущности предметной области KMP (`Receipt`, `KkmInfo`, `ShiftInfo`) и интерфейсы портов (`StoragePort`, `ClockPort`, `DeliveryPort`).
2. **`core-data`**: Реализации портов хранения, отправки документов в ОФД, политик повторных попыток и межпроцессных блокировок.
3. **`core-presentation`**: Фасад презентационного слоя (`SuperkassaApi`), предоставляющий методы интеграции ядра с внешними клиентами.

Подключите единый KMP модуль в зависимости вашего Gradle-проекта:

```kotlin
dependencies {
    // Для мультиплатформенных (KMP) проектов
    implementation("io.github.texport:superkassa-core:1.0.3")
    
    // Для классических JVM-проектов (например, сервер)
    implementation("io.github.texport:superkassa-core-jvm:1.0.3")
}
```

#### Apple Swift Package Manager

Для iOS-проектов ядро скомпилировано в бинарный фреймворк `SuperkassaCore.xcframework` и распространяется через Swift Package Manager. Добавьте зависимость в ваш `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/texport/superkassa-core", from: "1.0.3")
]
```

---

## Quick Start / Usage

Here is a quick example of how to initialize and interact with `SuperkassaApi` in your application:

```kotlin
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.KkmInitDirectRequest
import io.github.texport.superkassa.core.presentation.api.model.ReceiptSellRequest
import io.github.texport.superkassa.core.presentation.api.model.ReceiptItemDto
import io.github.texport.superkassa.core.presentation.api.model.ReceiptPaymentDto

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
            ReceiptItemDto(
                name = "Фискальный товар",
                price = 1500.0,
                quantity = 1L,
                vatGroup = "VAT_12",
                measureUnitCode = "796"
            )
        ),
        payments = listOf(
            ReceiptPaymentDto(type = "CASH", sum = 1500.0)
        ),
        idempotencyKey = "unique-receipt-key-1"
    )
)

println("Receipt registered successfully with ticket number: ${sellResult.ticketNumber}")
```

## Architecture Boundary

The project follows a strict Clean Architecture boundary design:

- **core-domain (Entities & Use Cases):** Contains the core business models and interfaces. There is absolutely no external dependency on presentation logic, and all domain models are completely decoupled from serialization logic (e.g. no `@Serializable` annotations).
- **core-data (Adapters & Infrastructure):** Implements ports for storage, OFD network connections, and offline queuing. It relies only on `core-domain`.
- **core-presentation (API & DTOs):** Exposes a clean facade layer via `SuperkassaApi`. All serialization logic and API request/response structures are declared here as decoupled DTOs (e.g. `UserRoleDto`, `TaxRegimeDto`), preventing serialization libraries or annotations from leaking into the domain layer.
