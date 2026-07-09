package io.github.texport.superkassa.receiptrenderer.impl.renderer.style

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.ResourceLoader

internal object SharedStyles {
    val SHARED_CSS: String by lazy {
        ResourceLoader.readText("/css/shared_styles.css") ?: ""
    }
}
