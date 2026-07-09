package kz.mybrain.superkassa.offline_queue.application.model

internal class QueueDispatchException(val error: QueueErrorMessage) : IllegalArgumentException(error.compact())
