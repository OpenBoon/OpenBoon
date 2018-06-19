package com.zorroa.archivist.service

import com.zorroa.archivist.repository.AssetDao
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.sdk.services.AssetId
import com.zorroa.archivist.sdk.services.AssetService
import com.zorroa.archivist.sdk.services.AssetSpec
import com.zorroa.archivist.sdk.services.StorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
@Transactional
class AssetServiceInternalImpl @Autowired constructor (
        val assetDao: AssetDao,
        val storageService : StorageService,
        val properties: ApplicationProperties,
        val tx: TransactionEventManager) : AssetService {

    override fun create(spec: AssetSpec) : AssetId {
        val asset = assetDao.create(spec)
        tx.afterCommit {
            storageService.createBucket(asset)
            storageService.storeMetadata(asset, spec.document)
        }

        return asset
    }

    override fun get(id: UUID) : AssetId {
        return assetDao.getId(id)
    }
}
