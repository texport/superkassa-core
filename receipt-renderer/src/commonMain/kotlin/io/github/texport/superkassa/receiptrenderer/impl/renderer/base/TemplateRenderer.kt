package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

internal object TemplateRenderer {
    private val templateCache = mutableMapOf<String, String>()

    fun render(templateName: String, placeholders: Map<String, String>): String {
        val template = templateCache.getOrPut(templateName) {
            ResourceLoader.readText("/templates/$templateName")?.trim()
                ?: throw IllegalArgumentException("Template /templates/$templateName not found")
        }
        var result = template
        placeholders.forEach { (k, v) ->
            result = result.replace("/*{{$k}}*/", v)
            result = result.replace("{{$k}}", v)
        }
        return result
    }
}
