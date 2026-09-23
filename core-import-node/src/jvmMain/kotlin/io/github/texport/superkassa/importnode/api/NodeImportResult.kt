package io.github.texport.superkassa.importnode.api

/** Чем кончился перенос данных узла. */
sealed interface NodeImportResult {

    /** Данные узла перенесены и сверены; каталог кассы готов к `createSuperkassa`. */
    data class Imported(val report: NodeImportReport) : NodeImportResult

    /** Перенос сделан раньше; повторный вызов ничего не трогает. */
    data object AlreadyImported : NodeImportResult

    /** У узла нет базы: переносить нечего, касса начинает с чистого каталога. */
    data object NoNodeData : NodeImportResult
}
