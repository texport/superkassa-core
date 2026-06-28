package kz.mybrain.superkassa.core.data.adapter

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfdConfigAdapterTest {
    private val adapter = OfdConfigAdapter()

    @Test
    fun testHasEndpointValid() {
        // "KAZAKHTELECOM" is a valid provider in OfdProvider
        // "PROD" is a valid env in OfdEnvironment
        assertTrue(adapter.hasEndpoint("KAZAKHTELECOM", "PROD"))
    }

    @Test
    fun testHasEndpointInvalidProvider() {
        assertFalse(adapter.hasEndpoint("NON_EXISTENT_PROVIDER", "PROD"))
    }

    @Test
    fun testHasEndpointInvalidEnvironment() {
        assertFalse(adapter.hasEndpoint("KAZAKHTELECOM", "NON_EXISTENT_ENV"))
    }

    @Test
    fun testHasEndpointMissingFromProviderEndpoints() {
        // "DEV" is a valid environment, but KAZAKHTELECOM doesn't configure a host/port for it
        assertFalse(adapter.hasEndpoint("KAZAKHTELECOM", "DEV"))
    }

    @Test
    fun testHasEndpointCaseInsensitive() {
        assertTrue(adapter.hasEndpoint("KAZAKHTELECOM", "prod"))
        assertTrue(adapter.hasEndpoint("KAZAKHTELECOM", "PrOd"))
    }
}
