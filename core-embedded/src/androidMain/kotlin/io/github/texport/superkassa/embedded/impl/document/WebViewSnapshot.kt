package io.github.texport.superkassa.embedded.impl.document

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Снимок страницы системным WebView вне экрана.
 *
 * WebView живёт в главном потоке, поэтому работа уходит туда, а вызывающий
 * ждёт результат с пределом. Не дождался — снимок отменяется и вызывающий
 * получает отказ, а не пустую картинку и не вечное ожидание.
 */
internal object WebViewSnapshot {
    private const val TIMEOUT_S = 15L

    /**
     * Рисует [page] в растр шириной [widthPx]; высота — по содержимому.
     *
     * Вызывать не из главного потока: вызов из него ждал бы сам себя.
     */
    fun take(context: Context, page: String, paperWidthMm: Double, widthPx: Int): Bitmap {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Documents must not be rendered on the main thread" }
        val done = CountDownLatch(1)
        val result = AtomicReference<Result<Bitmap>>()
        val capture = AtomicReference<PageCapture>()
        val finish: (Result<Bitmap>) -> Unit = {
            result.set(it)
            done.countDown()
        }
        val scale = widthPx / (paperWidthMm * PrintForm.PX_PER_MM)
        val main = Handler(Looper.getMainLooper())
        main.post {
            runCatching { PageCapture(context, widthPx, scale, finish).also(capture::set).start(page) }
                .onFailure { failure -> finish(Result.failure(failure)).also { capture.get()?.close() } }
        }
        if (!done.await(TIMEOUT_S, TimeUnit.SECONDS)) {
            main.post { capture.get()?.close() }
            error("WebView did not render the document in ${TIMEOUT_S}s")
        }
        return result.get().getOrThrow()
    }
}
