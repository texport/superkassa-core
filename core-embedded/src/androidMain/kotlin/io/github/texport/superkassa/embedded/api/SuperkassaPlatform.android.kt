package io.github.texport.superkassa.embedded.api

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import io.github.texport.superkassa.embedded.impl.document.WebViewDocuments
import io.github.texport.superkassa.embedded.impl.print.UnsupportedPrinter
import java.io.File

/**
 * Платформа Android.
 *
 * Документы рисует системный WebView — движок самой ОС, а не браузер
 * в поставке. Печать на принтеры Android пока не сделана и отказывает.
 *
 * @param context контекст приложения; хранится только контекст приложения.
 * @param dataDir каталог данных кассы; по умолчанию во внутренней памяти приложения.
 */
actual class SuperkassaPlatform(
    context: Context,
    dataDir: String = File(context.filesDir, "superkassa").absolutePath
) {
    private val appContext: Context = context.applicationContext

    internal actual val dataDir: String = dataDir

    internal actual fun databaseBuilder(path: String): RoomDatabase.Builder<SuperkassaAppDatabase> =
        Room.databaseBuilder<SuperkassaAppDatabase>(appContext, path)

    internal actual fun documents(): DocumentConvertPort = WebViewDocuments(appContext)

    internal actual fun printer(): DocumentPrinter = UnsupportedPrinter("Android")
}
