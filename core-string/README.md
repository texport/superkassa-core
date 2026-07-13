# superkassa-core-string

[![Version](https://img.shields.io/badge/version-1.0.3-blue.svg)](https://github.com/texport/superkassa-core/releases)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)](https://github.com/texport/superkassa-core/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

## Documentation in English

`superkassa-core-string` is the multiplatform localization and string resource module for the **Superkassa** cash register core. It provides consistent translations and error messages.

### Key Features
- **Trilingual Localization**: Out-of-the-box support for Russian, Kazakh, and English localization strings.
- **Resource Bundles**: Decoupled multiplatform string loading mechanism.

#### Integration
Add the dependency to your KMP project's `commonMain`:
```kotlin
implementation("io.github.texport:superkassa-core-string:1.0.3")
```

---

## Документация на русском языке

`superkassa-core-string` — это мультиплатформенный модуль локализации и строковых ресурсов ядра фискальной системы **Superkassa**. Он обеспечивает единые переводы и сообщения об ошибках.

### Ключевые возможности
- **Трехъязычная локализация**: Встроенная поддержка сообщений на русском, казахском и английском языках.
- **Управление ресурсами**: Абстрагированный механизм мультиплатформенной загрузки строк на различных ОС.

#### Интеграция
Добавьте зависимость в общий набор исходного кода `commonMain` вашего Gradle-проекта:
```kotlin
implementation("io.github.texport:superkassa-core-string:1.0.3")
```
