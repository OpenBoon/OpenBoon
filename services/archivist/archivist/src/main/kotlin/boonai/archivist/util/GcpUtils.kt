package boonai.archivist.util

import boonai.archivist.config.ArchivistConfiguration
import boonai.archivist.service.PubSubSubscriberService
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.auth.oauth2.ComputeEngineCredentials
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.Publisher
import com.google.pubsub.v1.ProjectTopicName
import io.grpc.ManagedChannelBuilder
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Functions for working with GCP
 */
interface GcpUtils

/**
 * Create a Google PubSub Publisher
 */
fun createPubSubPublisher(topicId: String): Publisher {
    return if (!isPubSubEmulation()) {
        val topicName = ProjectTopicName.of(ServiceOptions.getDefaultProjectId(), topicId)
        Publisher.newBuilder(topicName).build()
    } else {
        val hostport = getPubSubEmulationHost()
        val channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build()
        val channelProvider: TransportChannelProvider =
            FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
        val credentialsProvider: CredentialsProvider = NoCredentialsProvider.create()

        // Set the channel and credentials provider when creating a `Publisher`.
        // Similarly for Subscriber
        Publisher.newBuilder("projects/localdev/topics/$topicId")
            .setChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build()
    }
}

/**
 * Return true if there is pubsub emulation.
 */
fun isPubSubEmulation(): Boolean {
    return ArchivistConfiguration.unittest || System.getenv("PUBSUB_EMULATOR_HOST") != null
}

/**
 * Return the hostname:port of the pubsub emulator host or null.
 */
fun getPubSubEmulationHost(): String? {
    return if (isPubSubEmulation()) {
        System.getenv("PUBSUB_EMULATOR_HOST") ?: PubSubSubscriberService.UNITTEST_HOST
    } else {
        null
    }
}

/**
 * Load the Archivist credentials file from it's in-production location.
 */
fun loadGcpCredentials(): GoogleCredentials {
    return loadGcpCredentials("/secrets/gcs/credentials.json")
}

/**
 * Load an alnternative credentials file.
 */
fun loadGcpCredentials(path: String): GoogleCredentials {
    val credsFile = Paths.get("/secrets/gcs/credentials.json")

    return if (Files.exists(credsFile)) {
        GoogleCredentials.fromStream(FileInputStream(credsFile.toFile()))
    } else {
        ComputeEngineCredentials.create()
    }
}
