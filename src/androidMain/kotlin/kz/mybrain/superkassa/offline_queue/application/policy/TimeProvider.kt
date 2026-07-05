package kz.mybrain.superkassa.offline_queue.application.policy

actual fun currentTimeMillis(): Long {
    return java.lang.System.currentTimeMillis()
}
