package io.github.texport.superkassa.core.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import kotlin.test.Test

class ArchitectureTest {

    @Test
    fun `check Clean Architecture boundaries`() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .withImportOption { location ->
                !location.contains("/test/") &&
                !location.contains("/jvmTest/") &&
                !location.contains("/commonTest/") &&
                !location.contains("/appleTest/") &&
                !location.contains("/iosTest/")
            }
            .importPackages("io.github.texport.superkassa")

        layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("io.github.texport.superkassa..")
            .layer("Domain").definedBy("io.github.texport.superkassa.core.domain..")
            .layer("Data").definedBy("io.github.texport.superkassa.core.data.impl..")
            .layer("Engine").definedBy("io.github.texport.superkassa.core.data.api..")
            .layer("Presentation").definedBy("io.github.texport.superkassa.core.presentation..")
            .layer("String").definedBy("io.github.texport.superkassa.core.string..")
            
            .layer("DeliveryApi").definedBy("io.github.texport.superkassa.delivery.api..")
            .layer("DeliveryImpl").definedBy("io.github.texport.superkassa.delivery.impl..")
            
            .layer("OfflineQueueApi").definedBy("io.github.texport.superkassa.offlinequeue.api..")
            .layer("OfflineQueueImpl").definedBy("io.github.texport.superkassa.offlinequeue.impl..")
            
            .layer("ReceiptRendererApi").definedBy("io.github.texport.superkassa.receiptrenderer.api..")
            .layer("ReceiptRendererImpl").definedBy("io.github.texport.superkassa.receiptrenderer.impl..")
            
            .layer("DatabaseApi").definedBy("io.github.texport.superkassa.coredatabase.api..")
            .layer("DatabaseImpl").definedBy("io.github.texport.superkassa.coredatabase.impl..")

            .whereLayer("String").mayNotAccessAnyLayer()
            .whereLayer("Domain").mayOnlyAccessLayers("String")
            .whereLayer("Data").mayOnlyAccessLayers("Domain", "String", "DeliveryApi", "OfflineQueueApi", "ReceiptRendererApi", "DatabaseApi")
            .whereLayer("Presentation").mayOnlyAccessLayers("Domain", "String")
            .whereLayer("Engine").mayOnlyAccessLayers("Domain", "String", "Presentation", "Data", "DeliveryImpl", "ReceiptRendererImpl", "DatabaseApi")
            
            .whereLayer("DeliveryApi").mayOnlyAccessLayers("String", "Domain", "DeliveryImpl")
            .whereLayer("DeliveryImpl").mayOnlyAccessLayers("String", "Domain", "DeliveryApi")
            .whereLayer("DeliveryImpl").mayOnlyBeAccessedByLayers("DeliveryApi", "DeliveryImpl", "Engine")
            
            .whereLayer("OfflineQueueApi").mayOnlyAccessLayers("String", "OfflineQueueImpl")
            .whereLayer("OfflineQueueImpl").mayOnlyAccessLayers("String", "OfflineQueueApi")
            .whereLayer("OfflineQueueImpl").mayOnlyBeAccessedByLayers("OfflineQueueApi", "OfflineQueueImpl")
            
            .whereLayer("ReceiptRendererApi").mayOnlyAccessLayers("Domain", "String", "ReceiptRendererImpl")
            .whereLayer("ReceiptRendererImpl").mayOnlyAccessLayers("Domain", "String", "ReceiptRendererApi")
            .whereLayer("ReceiptRendererImpl").mayOnlyBeAccessedByLayers("ReceiptRendererApi", "ReceiptRendererImpl", "Engine")

            .whereLayer("DatabaseApi").mayOnlyAccessLayers("Domain", "String", "OfflineQueueApi", "DatabaseImpl")
            .whereLayer("DatabaseImpl").mayOnlyAccessLayers("Domain", "String", "OfflineQueueApi", "DatabaseApi")
            .check(importedClasses)
    }
}
