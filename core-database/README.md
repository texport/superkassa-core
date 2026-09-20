# superkassa-core-database

[![Version](https://img.shields.io/badge/version-1.2.0-blue.svg)](https://github.com/texport/superkassa-core)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Модуль локального хранилища данных на базе **Room KMP** (`androidx.room` + `sqlite-bundled`) для библиотеки `superkassa-core`.

## Возможности
- Хранение параметров ККМ, смен, фискальных документов и офлайн-очереди.
- 100% реализация `StoragePort` и `QueueStoragePort` без написания платформа-зависимого кода.
- Кроссплатформенная обработка SQLite на JVM, Android и iOS.
