package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration

/** Все шаги схемы по порядку: и старое, и строгое открытие базы идут по одному списку. */
internal val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_DROP_USER_PIN,
    MIGRATION_KKM_OFD_ADDRESS,
    MIGRATION_DROP_KKM_OFD_ADDRESS,
    MIGRATION_KKM_NAME,
    MIGRATION_DOCUMENT_REFUSAL_TEXT,
    MIGRATION_IDEMPOTENCY_AND_RECEIPT_URL,
    MIGRATION_KKM_BRANDING,
    MIGRATION_PIN_ATTEMPTS,
    MIGRATION_KKM_TAXPAYER_COLUMNS
)
