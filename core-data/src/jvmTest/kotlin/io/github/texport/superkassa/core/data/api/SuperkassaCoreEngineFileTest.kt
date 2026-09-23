package io.github.texport.superkassa.core.data.api

import io.github.texport.superkassa.coredatabase.api.StorageOpenException
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Промышленные сборки ядра держат базу в файле по заданному пути
 * и не поднимаются на базе, которая не открылась.
 */
class SuperkassaCoreEngineFileTest {
    private val dir: File = createTempDirectory("engine-").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `каждая сборка заводит базу в файле, даже если в пути есть test`() {
        val paths = listOf("test_prod.db", "test_desktop.db", "test_android.db", "test_ios.db").map { File(dir, it).path }
        SuperkassaCoreEngine.createProduction(paths[0])
        SuperkassaCoreEngine.createDesktop(paths[1])
        SuperkassaCoreEngine.createAndroid(paths[2])
        SuperkassaCoreEngine.createIos(paths[3])
        paths.forEach { assertTrue(File(it).isFile, "no database file at $it") }
    }

    @Test
    fun `база не открылась — ядро не поднимается, файл цел`() {
        val database = File(dir, "superkassa.db")
        val garbage = "not an SQLite database ".repeat(200).toByteArray()
        database.writeBytes(garbage)

        assertFailsWith<StorageOpenException> { SuperkassaCoreEngine.createProduction(database.path) }
        assertContentEquals(garbage, database.readBytes())
    }
}
