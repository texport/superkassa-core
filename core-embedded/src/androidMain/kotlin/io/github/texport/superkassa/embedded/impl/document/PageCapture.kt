package io.github.texport.superkassa.embedded.impl.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View.MeasureSpec
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Снимок одной страницы в главном потоке.
 *
 * Шаги идут по подтверждениям WebView, а не по паузам:
 * 1. страница загружена — ждём, пока её состояние будет готово к отрисовке;
 * 2. готово — высота документа известна, и вид вытягивается во всю ленту;
 * 3. вытянутый вид готов — лента рисуется в растр целиком.
 *
 * Готовность — `VisualStateCallback`: следующий кадр после него отражает всю
 * страницу. После [close] ни один шаг уже не выполняется.
 *
 * @param widthPx ширина растра.
 * @param scale пикселей растра на точку CSS.
 * @param done получает растр или причину отказа, ровно один раз.
 */
internal class PageCapture(
    context: Context,
    private val widthPx: Int,
    private val scale: Double,
    private val done: (Result<Bitmap>) -> Unit
) : AutoCloseable {
    private val window = OffscreenWindow(context, widthPx)
    private val view: WebView
    private var closed = false

    init {
        // Вид выше окна: без этого WebView рисует только видимую в окне часть.
        WebView.enableSlowWholeDocumentDraw()
        view = runCatching { WebView(window.context) }.getOrElse {
            window.close()
            throw it
        }
    }

    /** Загружает страницу; дальше шаги идут сами. */
    fun start(page: String) {
        view.settings.javaScriptEnabled = false
        view.setBackgroundColor(Color.WHITE)
        view.setInitialScale((scale * PERCENT).roundToInt())
        view.webViewClient = Client()
        // Низкий вид на загрузке: высота документа не подтягивается к высоте окна.
        window.show(view, widthPx, LOADING_HEIGHT_PX)
        view.loadDataWithBaseURL(WebFonts.BASE_URL, WebFonts.styled(page), "text/html", "UTF-8", null)
    }

    private fun fitToContent() {
        val heightPx = ceil(view.contentHeight * scale).toInt()
        check(heightPx > 0) { "WebView reported an empty document" }
        view.layoutParams = view.layoutParams.apply { height = heightPx }
        view.measure(exactly(widthPx), exactly(heightPx))
        view.layout(0, 0, widthPx, heightPx)
        whenReady { draw(heightPx) }
    }

    private fun draw(heightPx: Int) {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        view.draw(Canvas(bitmap))
        finish(Result.success(bitmap))
    }

    private fun whenReady(step: () -> Unit) = view.postVisualStateCallback(
        0,
        object : WebView.VisualStateCallback() {
            override fun onComplete(requestId: Long) = attempt(step)
        }
    )

    private fun attempt(step: () -> Unit) {
        if (!closed) runCatching(step).onFailure { finish(Result.failure(it)) }
    }

    private fun finish(result: Result<Bitmap>) {
        if (closed) return
        close()
        done(result)
    }

    override fun close() {
        if (closed) return
        closed = true
        view.destroy()
        window.close()
    }

    private fun exactly(px: Int): Int = MeasureSpec.makeMeasureSpec(px, MeasureSpec.EXACTLY)

    private inner class Client : WebViewClient() {
        override fun onPageFinished(finished: WebView, url: String?) = whenReady(::fitToContent)

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            WebFonts.response(request.url.toString())
    }

    private companion object {
        const val PERCENT = 100.0
        const val LOADING_HEIGHT_PX = 1
    }
}
