package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.repository.AssetDao
import com.zorroa.common.domain.AssetId
import com.zorroa.common.domain.AssetSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


interface AssetService {

    /**
     * Create an asset from the given asset spec.  If the location already exists
     * then reprocess the asset.
     */
    fun create(spec: AssetSpec) : com.zorroa.common.domain.AssetId

    /**
     * Get an asset by its unique ID.
     */
    fun get(id: UUID) : com.zorroa.common.domain.AssetId
}

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
