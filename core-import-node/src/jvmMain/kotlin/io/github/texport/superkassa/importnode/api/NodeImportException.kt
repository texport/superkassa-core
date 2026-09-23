package io.github.texport.superkassa.importnode.api

/**
 * Перенос не сделан, и каталог кассы остался таким, каким был.
 *
 * Сообщение называет причину: узел ещё работает, база узла другой версии,
 * запись не ложится в базу кассы без потери, сверка разошлась. Приложение
 * в этом случае кассу не поднимает: пустая касса выглядела бы как
 * потерянные смены и чеки и толкала бы к повторной регистрации.
 */
class NodeImportException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
