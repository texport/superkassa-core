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
            .importPackages("io.github.texport.superkassa.core")

        layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("io.github.texport.superkassa.core..")
            .layer("Domain").definedBy("io.github.texport.superkassa.core.domain..")
            .layer("Data").definedBy("io.github.texport.superkassa.core.data..")
            .layer("Presentation").definedBy("io.github.texport.superkassa.core.presentation..")
            .layer("String").definedBy("io.github.texport.superkassa.core.string..")

            .whereLayer("String").mayNotAccessAnyLayer()
            .whereLayer("Domain").mayOnlyAccessLayers("String")
            .whereLayer("Data").mayOnlyAccessLayers("Domain", "String")
            .whereLayer("Presentation").mayOnlyAccessLayers("Domain", "String")
            .check(importedClasses)
    }
}
