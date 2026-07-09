# superkassa-receipt-renderer

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/superkassa-receipt-renderer.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.texport/superkassa-receipt-renderer)
[![Version](https://img.shields.io/badge/version-1.0.3-blue.svg)](https://github.com/texport/superkassa-receipt-renderer/releases)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)](https://github.com/texport/superkassa-receipt-renderer/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI Build](https://img.shields.io/github/actions/workflow/status/texport/superkassa-receipt-renderer/ci.yml?branch=main&label=CI%20Build)](https://github.com/texport/superkassa-receipt-renderer/actions)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

## Documentation in English

`superkassa-receipt-renderer` is the Kotlin Multiplatform (KMP) receipt templating and HTML rendering library for the **Superkassa** fiscal cash register core.

### Key Features
- **HTML Receipt Rendering**: Renders full-featured print-ready HTML files for sell, buy, refund, and shift reports.
- **Multilingual Support**: Supports trilingual ticket rendering (Russian, Kazakh, English).
- **Customizable Styling**: Packages embedded CSS assets with flexible dark/light theme options and custom branding capabilities.

#### Integration
Add the dependency to your KMP project's `commonMain`:
```kotlin
implementation("io.github.texport:superkassa-receipt-renderer:1.0.3")
```

---

## Документация на русском языке

`superkassa-receipt-renderer` — это мультиплатформенная библиотека (Kotlin Multiplatform / KMP) шаблонизации чеков и рендеринга HTML для ядра фискальной системы **Superkassa**.

### Ключевые возможности
- **Рендеринг HTML-чеков**: Генерация готовых к печати HTML-файлов для чеков продаж, покупок, возвратов и отчетов о сменах.
- **Поддержка трех языков**: Полный перевод печатных форм чеков на русский, казахский и английский языки.
- **Кастомизация стилей**: Использование встроенных CSS-стилей с поддержкой темных/светлых тем оформления и брендирования.

#### Интеграция
Добавьте зависимость в общий набор исходного кода `commonMain` вашего Gradle-проекта:
```kotlin
implementation("io.github.texport:superkassa-receipt-renderer:1.0.3")
```
