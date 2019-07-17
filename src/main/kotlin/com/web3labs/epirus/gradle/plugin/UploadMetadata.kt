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
import javax.ws.rs.core.Response.Status.OK

@CacheableTask
open class UploadMetadata : SourceTask() {

    @field:Input
    lateinit var url: String

    init {
//        outputs.upToDateWhen { inputs }
    }

    @TaskAction
    fun uploadMetadata() {

        // Target the Epirus metadata endpoint
        val client = ClientBuilder.newClient()

        val target = client.target(url)
            .register(MultiPartFeature::class.java)
            .path("metadata")

        val web3jExtension = project["web3j"] as Web3jExtension

        source.asIterable()
            .map { it.contractName to it }
            .filter {
                with(web3jExtension) {
                    // Filter included / excluded contracts
                    if (includedContracts.isNotEmpty()) {
                        includedContracts.contains(it.first)
                    } else {
                        !excludedContracts.contains(it.first)
                    }
                }
            }.forEach {
                logger.info("Uploading metadata for $it.first...")

                val bodyPart = FileDataBodyPart("file", it.second)
                val multiPart = FormDataMultiPart().bodyPart(bodyPart)
                val entity = Entity.entity(multiPart, multiPart.mediaType)

                target.request().post(entity).apply {
                    when (statusInfo.toEnum()) {
                        OK -> logger.info("${it.first} metadata uploaded.")
                        else -> logger.warn(
                            "Could not upload metadata for " +
                                    "${it.first}: ${statusInfo.reasonPhrase}"
                        )
                    }
                }
            }
        client.close()
    }

    private val File.contractName
        get() = nameWithoutExtension.removeSuffix("_meta")
}
