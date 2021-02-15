package com.zorroa.zmlp.service.storage

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder

open class StorageProperties {

    lateinit var bucket: String

    var createBucket: Boolean = false

    var accessKey: String? = null

    var secretKey: String? = null

    var url: String? = null
}

class SystemStorageException : RuntimeException {
    constructor(message: String, ex: Exception?) : super(message, ex) {}
    constructor(message: String) : super(message) {}
    constructor(ex: Exception) : super(ex) {}
}

fun getS3Client(properties: StorageProperties): AmazonS3 {

    val credentials: AWSCredentials = BasicAWSCredentials(
        properties.accessKey, properties.secretKey
    )

    val clientConfiguration = ClientConfiguration()
    clientConfiguration.signerOverride = "AWSS3V4SignerType"

    return AmazonS3ClientBuilder
        .standard()
        .withEndpointConfiguration(
            AwsClientBuilder.EndpointConfiguration(
                properties.url, Regions.DEFAULT_REGION.name
            )
        )
        .withPathStyleAccessEnabled(true)
        .withClientConfiguration(clientConfiguration)
        .withCredentials(AWSStaticCredentialsProvider(credentials))
        .build()
}
