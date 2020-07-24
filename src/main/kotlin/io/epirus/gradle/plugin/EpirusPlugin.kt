package io.epirus.gradle.plugin

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.IdentityFileResolver
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.web3j.gradle.plugin.Web3jPlugin
import org.web3j.solidity.gradle.plugin.OutputComponent
import org.web3j.solidity.gradle.plugin.SolidityExtension
import org.web3j.solidity.gradle.plugin.SoliditySourceSet
import java.io.File

class EpirusPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply(Web3jPlugin::class.java)
        target.extensions.create(EpirusExtension.NAME, EpirusExtension::class.java)

        // Add metadata output component by default
        val solidityExtension = target["solidity"] as SolidityExtension
        solidityExtension.setOutputComponents(*solidityExtension.outputComponents, OutputComponent.METADATA)

        target.afterEvaluate {
            val plugin = target.convention.getPlugin(JavaPluginConvention::class.java)
            plugin.sourceSets.all { sourceSet -> configure(target, sourceSet) }
            configureCliInstaller(it)
        }
    }

    private fun configureCliInstaller(project: Project) {
        val installerPath = "${project.buildDir}/tmp/epirus-installer.sh"

        project.tasks.register("installEpirusCli", Download::class.java).configure {
            it.group = EpirusExtension.NAME
            it.src("http://get.epirus.io")
            it.dest(File(installerPath))
            it.onlyIfModified(true)
            it.doLast {
                project.exec { task ->
                    task.commandLine("chmod", "+x", installerPath)
                    task.commandLine(installerPath)
                }
            }
        }
    }

    private fun configure(project: Project, sourceSet: SourceSet) {
        val srcSetName = if (sourceSet.name == "main") "" else sourceSet.name.capitalize()

        project.tasks.register("upload${srcSetName}Metadata", UploadMetadata::class.java).configure {
            val epirusExtension = project[EpirusExtension.NAME] as EpirusExtension
            it.dependsOn("compile${srcSetName}Solidity")
            it.source = buildMetaDirectorySet(sourceSet)
            it.group = EpirusExtension.NAME
            it.url = epirusExtension.url
        }
    }

    private fun buildMetaDirectorySet(sourceSet: SourceSet): SourceDirectorySet {
        val displayName = "${sourceSet.name.capitalize()} Solidity Metadata"

        @Suppress("DEPRECATION")
        val directorySet = DefaultSourceDirectorySet(
            sourceSet.name, displayName,
            IdentityFileResolver(),
            DefaultDirectoryFileTreeFactory()
        )
        directorySet.srcDir(buildSolidityOutputDir(sourceSet))
        directorySet.include("**/*_meta.json")
        return directorySet
    }

    private fun buildSolidityOutputDir(sourceSet: SourceSet): File {
        val convention = sourceSet["convention"] as Convention
        val soliditySourceSet = convention.plugins[SoliditySourceSet.NAME] as SoliditySourceSet

        @Suppress("UnstableApiUsage")
        return soliditySourceSet.solidity.outputDir
    }

    companion object {
        const val ID = "com.web3labs.epirus"
    }
}

