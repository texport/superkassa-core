package io.github.texport.superkassa.embedded.impl.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View.MeasureSpec
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/**
 * Снимок страницы системным WebView вне экрана.
 *
 * WebView живёт в главном потоке, поэтому работа уходит туда, а вызывающий
 * ждёт результат с пределом. Не дождался — отказ, а не пустая картинка.
 */
internal object WebViewSnapshot {
    private const val TIMEOUT_S = 15L
    private const val SETTLE_MS = 150L
    private const val PERCENT = 100.0

    fun take(context: Context, page: String, paperWidthMm: Double, widthPx: Int): Bitmap {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Documents must not be rendered on the main thread" }
        val done = CountDownLatch(1)
        val result = AtomicReference<Result<Bitmap>>()
        val finish: (Result<Bitmap>) -> Unit = {
            result.set(it)
            done.countDown()
        }
        Handler(Looper.getMainLooper()).post {
            runCatching { load(context, page, paperWidthMm, widthPx, finish) }.onFailure { finish(Result.failure(it)) }
        }
        check(done.await(TIMEOUT_S, TimeUnit.SECONDS)) { "WebView did not render the document in ${TIMEOUT_S}s" }
        return result.get().getOrThrow()
    }

    private fun load(context: Context, page: String, paperWidthMm: Double, widthPx: Int, done: (Result<Bitmap>) -> Unit) {
        WebView.enableSlowWholeDocumentDraw()
        val view = WebView(context)
        view.settings.javaScriptEnabled = false
        view.setInitialScale((PERCENT * widthPx / (paperWidthMm * PrintForm.PX_PER_MM)).roundToInt())
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(finished: WebView, url: String?) {
                finished.postDelayed({ done(runCatching { capture(finished, widthPx) }) }, SETTLE_MS)
            }
        }
        measure(view, widthPx)
        view.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null)
    }

    private fun capture(view: WebView, widthPx: Int): Bitmap {
        try {
            measure(view, widthPx)
            val bitmap = Bitmap.createBitmap(widthPx, view.measuredHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            return bitmap
        } finally {
            view.destroy()
        }
    }

    private fun measure(view: WebView, widthPx: Int) {
        view.measure(
            MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, widthPx, view.measuredHeight)
    }
}
