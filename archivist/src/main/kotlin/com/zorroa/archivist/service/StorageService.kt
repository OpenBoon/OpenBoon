package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Bucket
import com.zorroa.archivist.sdk.config.ApplicationProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface StorageService {
    fun createBucket(name: String) : Bucket
}

@Service
class MinioStorageImpl @Autowired constructor (
        properties: ApplicationProperties) : StorageService {

    override fun createBucket(name: String) : Bucket {
        return Bucket(name)

    }
    /*
    private val client : MinioClient = MinioClient(
            properties.getString("archivist.ofs.endpoint"),
            properties.getString("archivist.ofs.accessKey"),
            properties.getString("archivist.ofs.secretKey"))

    override fun createBucket(name: String) : Bucket {
        if (!client.bucketExists(name)) {
            client.makeBucket(name)
        }
        return Bucket(name)
    }

    */
}
