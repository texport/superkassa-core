package io.github.texport.superkassa.core.data

import io.github.texport.superkassa.core.data.api.SuperkassaCoreEngine
import kotlin.test.Test
import kotlin.test.assertNotNull

class SuperkassaCoreEngineTest {

    @Test
    fun testFactoryMethods() {
        val defaultApi = SuperkassaCoreEngine.createDefault()
        assertNotNull(defaultApi)
    }
}
