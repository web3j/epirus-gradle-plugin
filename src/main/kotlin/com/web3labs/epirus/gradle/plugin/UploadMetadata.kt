package com.web3labs.epirus.gradle.plugin

import org.glassfish.jersey.media.multipart.FormDataMultiPart
import org.glassfish.jersey.media.multipart.MultiPartFeature
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.web3j.gradle.plugin.Web3jExtension
import java.io.File
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Response.Status.NOT_FOUND
import javax.ws.rs.core.Response.Status.OK

@CacheableTask
open class UploadMetadata : SourceTask() {

    @field:Input
    lateinit var url: String

    @TaskAction
    fun uploadMetadata() {

        // Target the Epirus metadata endpoint
        val client = ClientBuilder.newClient()

        val target = client.target(url)
            .register(MultiPartFeature::class.java)
            .path("metadata")

        val web3jExtension = project["web3j"] as Web3jExtension

        source.asIterable().filter {
            // Filter included / excluded contracts
            if (web3jExtension.includedContracts.isNotEmpty()) {
                web3jExtension.includedContracts.contains(it.contractName)
            } else {
                !web3jExtension.excludedContracts.contains(it.contractName)
            }
        }.forEach {
            val contractName = it.contractName

            val response = target.path("{swarmHash}")
                .resolveTemplate("swarmHash", "dddd") // FIXME calculate swarm hash
                .request()
                .get()

            when (response.statusInfo.toEnum()) {
                NOT_FOUND -> {
                    logger.info("Uploading metadata for $contractName...")

                    val bodyPart = FileDataBodyPart("file", it)
                    val multiPart = FormDataMultiPart().bodyPart(bodyPart)
                    val entity = Entity.entity(multiPart, multiPart.mediaType)

                    target.request().post(entity).apply {
                        when (statusInfo.toEnum()) {
                            OK -> logger.info("$contractName metadata uploaded.")
                            else -> logger.warn(
                                "Could not upload metadata for " +
                                        "$contractName: ${statusInfo.reasonPhrase}"
                            )
                        }
                    }
                }
                OK -> logger.info("$contractName metadata already exists, skipping.")
                else -> logger.warn(
                    "Epirus returned an unexpected error for " +
                            "$contractName: ${response.statusInfo.reasonPhrase}"
                )
            }
        }
        client.close()
    }

    private val File.contractName
        get() = nameWithoutExtension.removeSuffix("_meta")
}
