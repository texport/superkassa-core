package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

internal class MetadataBuilder(private val translateInlineKey: (String) -> String) {
    private val items = mutableListOf<Pair<String, String>>()

    fun add(key: String, value: String?) {
        val cleanValue = value?.trim() ?: ""
        if (cleanValue.isNotEmpty()) {
            items.add(translateInlineKey(key) to cleanValue.escaped())
        }
    }

    fun addRaw(key: String, value: String) {
        items.add(translateInlineKey(key) to value)
    }

    fun build(): List<Pair<String, String>> = items
}
