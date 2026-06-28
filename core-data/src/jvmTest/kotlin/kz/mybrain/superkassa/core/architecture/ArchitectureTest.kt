package kz.mybrain.superkassa.core.architecture

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
            .importPackages("kz.mybrain.superkassa.core")

        layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("kz.mybrain.superkassa.core..")
            .layer("Domain").definedBy("kz.mybrain.superkassa.core.domain..")
            .layer("Data").definedBy("kz.mybrain.superkassa.core.data..")
            .layer("Presentation").definedBy("kz.mybrain.superkassa.core.presentation..")
            
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Data").mayOnlyAccessLayers("Domain")
            .whereLayer("Presentation").mayOnlyAccessLayers("Domain")
            .check(importedClasses)
    }
}
