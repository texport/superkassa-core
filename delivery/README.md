# superkassa-delivery

[![Version](https://img.shields.io/badge/version-1.0.3-blue.svg)](https://github.com/texport/superkassa-delivery/releases)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)](https://github.com/texport/superkassa-delivery/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

A lightweight, clean-architecture Kotlin Multiplatform (KMP) library providing fiscal document delivery (Receipts, Reports) mechanisms through various channels (Email, SMS, etc.) for the **Superkassa** system.

It decouples the delivery logic from platform-specific APIs and networking layers by utilizing clean ports, ensuring full portability between JVM, Android, and iOS targets.

### Key Features
- **Trilingual Error Messaging**: Automatic support for English, Russian, and Kazakh error messages when delivery channels or adapters are missing.
- **Port-based Transport Layers**: Abstract ports (`DeliveryPort`) for transport channel implementations (Email, SMS) allowing the core module to remain 100% business-focused.
- **Robust Exception Handling**: Prevents network failures or client errors from bubbling up to business logic, converting errors into clean `DeliveryResult` structures.

---

#### Kotlin Multiplatform & Android
Add the dependency to your shared `commonMain` source set inside `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("io.github.texport:superkassa-delivery:1.0.3")
            }
        }
    }
}
```

#### Apple Swift Package Manager (SPM)
You can integrate this library directly into your iOS project using Xcode's Swift Package Manager:
1. In Xcode, select **File ➔ Add Package Dependencies...**
2. Enter the repository URL: `https://github.com/texport/superkassa-delivery.git`
3. Set the version rules to **Up to Next Major** starting with `1.0.3`.

---

### Usage Example

```kotlin
import io.github.texport.superkassa.delivery.api.DeliveryServiceApi
import io.github.texport.superkassa.delivery.api.createDeliveryServiceApi
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort

// 1. Implement transport-specific port adapters
class EmailDeliveryAdapter : DeliveryPort {
    override val channel = DeliveryChannel.EMAIL

    override fun send(request: DeliveryRequest): DeliveryResult {
        // Send email via your SMTP or API service
        return DeliveryResult(ok = true)
    }
}

// 2. Instantiate and configure the service
val emailAdapter = EmailDeliveryAdapter()
val deliveryServiceApi: DeliveryServiceApi = createDeliveryServiceApi(listOf(emailAdapter))

// 3. Perform document delivery
val result = deliveryServiceApi.deliver(
    DeliveryRequest(
        cashboxId = "cashbox-123",
        documentId = "document-456",
        channel = DeliveryChannel.EMAIL,
        destination = "client@example.com"
    )
)

if (result.ok) {
    println("Receipt successfully sent!")
} else {
    println("Failed to send receipt: ${result.message}")
}
```

---

## Архитектурные границы (Architecture Limits)

Библиотека разработана в соответствии с Clean Architecture и SOLID:
1. **Слой API (`io.github.texport.superkassa.delivery.api`)**: Содержит модели данных запросов/ответов и фасад-интерфейс `DeliveryServiceApi`. Внешние модули обращаются только к этому пакету.
2. **Слой Портов (`io.github.texport.superkassa.delivery.api.port`)**: Порт `DeliveryPort` служит точкой подключения для внешних транспортных адаптеров (например, SMS-шлюзы или почтовые сервисы). Реализации этих адаптеров лежат на стороне вызывающего приложения.
3. **Слой Реализации (`io.github.texport.superkassa.delivery.impl`)**: Внутренние компоненты (`DeliveryServiceApiImpl` и кроссплатформенный `Logger`) скрыты от внешнего мира модификатором `internal` и доступны только через фабрику `createDeliveryServiceApi`.
