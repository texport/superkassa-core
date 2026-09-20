package io.github.texport.superkassa.core.data

import io.github.texport.superkassa.core.data.api.SuperkassaCoreEngine
import kotlin.test.Test
import kotlin.test.assertNotNull

class SuperkassaCoreEngineTest {

    @Test
    fun testFactoryMethods() {
        val defaultApi = SuperkassaCoreEngine.createDefault()
        assertNotNull(defaultApi)

        val prodApi = SuperkassaCoreEngine.createProduction("test_prod_engine.db")
        assertNotNull(prodApi)

        val desktopApi = SuperkassaCoreEngine.createDesktop("test_desktop_engine.db")
        assertNotNull(desktopApi)

        val androidApi = SuperkassaCoreEngine.createAndroid("test_android_engine.db")
        assertNotNull(androidApi)

        val iosApi = SuperkassaCoreEngine.createIos("test_ios_engine.db")
        assertNotNull(iosApi)
    }
}
