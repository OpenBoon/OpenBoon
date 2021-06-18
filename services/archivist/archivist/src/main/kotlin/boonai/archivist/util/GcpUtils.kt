package boonai.archivist.util

import boonai.archivist.config.ArchivistConfiguration
import boonai.archivist.service.PubSubSubscriberService
import com.google.auth.oauth2.ComputeEngineCredentials
import com.google.auth.oauth2.GoogleCredentials
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Functions for working with GCP
 */
interface GcpUtils

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
