package io.github.texport.superkassa.importnode

import io.github.texport.superkassa.core.data.impl.util.Sha256
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Хеш пина у узла (`ServerPinHasherAdapter`: SHA-256 из JDK, строчный hex)
 * и у ядра (`Sha256PinHasherAdapter` → [Sha256]) один и тот же. Значит,
 * перенесённый хеш пускает кассира с прежним пином, и пин не нужен.
 */
class PinHashTest {

    @Test
    fun `хеш пина узла совпадает с хешем ядра`() {
        listOf("0000", "1234", "987654", "пин", "").forEach { pin ->
            assertEquals(NodeFixture.nodePinHash(pin), Sha256.hash(pin), "pin of length ${pin.length}")
        }
    }
}
