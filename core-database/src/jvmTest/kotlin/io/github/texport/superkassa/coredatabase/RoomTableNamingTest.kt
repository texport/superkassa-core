package io.github.texport.superkassa.coredatabase

import androidx.room.Entity
import androidx.room.Query
import io.github.texport.superkassa.coredatabase.impl.dao.CounterDao
import io.github.texport.superkassa.coredatabase.impl.dao.FiscalDocumentDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmUserDao
import io.github.texport.superkassa.coredatabase.impl.dao.PinAttemptDao
import io.github.texport.superkassa.coredatabase.impl.dao.QueueCommandDao
import io.github.texport.superkassa.coredatabase.impl.dao.ShiftDao
import io.github.texport.superkassa.coredatabase.impl.entity.CounterEntity
import io.github.texport.superkassa.coredatabase.impl.entity.FiscalDocumentEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmUserEntity
import io.github.texport.superkassa.coredatabase.impl.entity.PinAttemptEntity
import io.github.texport.superkassa.coredatabase.impl.entity.QueueCommandEntity
import io.github.texport.superkassa.coredatabase.impl.entity.ShiftEntity
import kotlin.test.Test
import kotlin.test.assertTrue

class RoomTableNamingTest {

    private val entityClasses: List<Class<*>> = listOf(
        KkmEntity::class.java,
        CounterEntity::class.java,
        FiscalDocumentEntity::class.java,
        KkmUserEntity::class.java,
        QueueCommandEntity::class.java,
        ShiftEntity::class.java,
        PinAttemptEntity::class.java
    )

    private val daoClasses: List<Class<*>> = listOf(
        KkmDao::class.java,
        CounterDao::class.java,
        FiscalDocumentDao::class.java,
        KkmUserDao::class.java,
        QueueCommandDao::class.java,
        ShiftDao::class.java,
        PinAttemptDao::class.java
    )

    @Test
    fun testEntityTableNamesMatchDaoQueries() {
        val registeredTableNames = entityClasses.mapNotNull { clazz ->
            val entityAnnotation = clazz.getAnnotation(Entity::class.java)
            entityAnnotation?.tableName?.ifEmpty { clazz.simpleName?.lowercase() }
        }.toSet()

        val fromRegex = Regex("""FROM\s+([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)
        val updateRegex = Regex("""UPDATE\s+([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)
        val intoRegex = Regex("""INTO\s+([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)

        for (daoClass in daoClasses) {
            for (method in daoClass.declaredMethods) {
                val queryAnnotation = method.getAnnotation(Query::class.java) ?: continue
                val sql = queryAnnotation.value

                val referencedTables = mutableListOf<String>()
                fromRegex.findAll(sql).forEach { referencedTables.add(it.groupValues[1]) }
                updateRegex.findAll(sql).forEach { referencedTables.add(it.groupValues[1]) }
                intoRegex.findAll(sql).forEach { referencedTables.add(it.groupValues[1]) }

                for (table in referencedTables) {
                    assertTrue(
                        table in registeredTableNames,
                        "Table '$table' referenced in ${daoClass.simpleName}.${method.name}() query \"$sql\" is not registered in any @Entity(tableName=...)! Registered tables: $registeredTableNames"
                    )
                }
            }
        }
    }
}
