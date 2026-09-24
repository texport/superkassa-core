package io.github.texport.superkassa.embedded.impl.document

import android.app.Presentation
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout

/**
 * Окно вне экрана: частный виртуальный дисплей приложения и Presentation на нём.
 *
 * WebView рисует страницу, только будучи прикреплённым к окну: неприкреплённому
 * Android не выполняет его отложенные задачи и не гарантирует кадр (условия —
 * в описании `WebView.postVisualStateCallback`). Частный дисплей разрешений не
 * требует и пользователю не виден. Плотность — 160 точек на дюйм: точка CSS
 * равна пикселю, и масштаб формы не зависит от экрана устройства.
 *
 * Создаётся, используется и закрывается в главном потоке.
 */
internal class OffscreenWindow(context: Context, widthPx: Int) : AutoCloseable {
    /** Кадры дисплея никто не смотрит: они сразу отпускаются, чтобы очередь буферов не встала. */
    private val frames = ImageReader.newInstance(widthPx, widthPx, PixelFormat.RGBA_8888, FRAMES).apply {
        setOnImageAvailableListener({ reader -> reader.acquireLatestImage()?.close() }, Handler(Looper.getMainLooper()))
    }
    private val display: VirtualDisplay = context.getSystemService(DisplayManager::class.java)
        .createVirtualDisplay(NAME, widthPx, widthPx, DisplayMetrics.DENSITY_DEFAULT, frames.surface, PRIVATE)
    private val presentation = Presentation(context, display.display)

    /** Контекст дисплея: в нём создаётся всё, что показывается в окне. */
    val context: Context get() = presentation.context

    /** Показывает [view] заданного размера; вид может быть больше дисплея. */
    fun show(view: View, widthPx: Int, heightPx: Int) {
        val root = FrameLayout(context)
        root.addView(view, FrameLayout.LayoutParams(widthPx, heightPx))
        presentation.setContentView(root)
        presentation.show()
    }

    override fun close() {
        presentation.dismiss()
        display.release()
        frames.close()
    }

    private companion object {
        const val NAME = "superkassa-documents"
        const val FRAMES = 2
        const val PRIVATE = 0
    }
}
