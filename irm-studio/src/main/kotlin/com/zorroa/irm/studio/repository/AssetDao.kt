package com.zorroa.irm.studio.repository

import com.zorroa.common.clients.RestClient
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.util.Json
import com.zorroa.irm.studio.rest.IndexRouteClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Repository
import java.util.*
import javax.annotation.PostConstruct

interface AssetDao<S> {
    fun create(spec: S) : Asset
    fun get(orgId: UUID, id: UUID) : Asset

    fun updateDocument(orgId: UUID, id: UUID, doc: Document) : Any
    fun updateDocument(orgId: UUID, id: String, doc: Document) : Any

    fun getDocument(orgId: UUID, id: UUID) : Document
    fun getDocument(orgId: UUID, id: String) : Document

}

@Configuration
@ConfigurationProperties("irm.cdv")
class CoreDataVaultSettings {
    var url: String? = null
}

data class CdvAsset (
        val documentGUID : UUID,
        val companyId: Long,
        val fileName: String
)

class CDVAssetSpec (
        val companyId: Long,
        val fileName: String
)

@Repository
class CoreDataVaultAssetRepositoryImpl : AssetDao<CDVAssetSpec> {

    @Autowired
    lateinit var cdvSettings: CoreDataVaultSettings

    lateinit var cdvClient: RestClient

    @Autowired
    lateinit var indexRouteClient: IndexRouteClient

    @PostConstruct
    fun setup() {
        cdvClient = RestClient(cdvSettings.url!!)
    }

    override fun create(spec: CDVAssetSpec) : Asset {
        val asset =  cdvClient.post("/companies/${spec.companyId}/assets/metadata", spec, CdvAsset::class.java)
        return Asset(asset.documentGUID, asset.companyId.toString(), asset.fileName)
    }

    override fun get(orgId: UUID, id: UUID) : Asset {
        val companyId = convertToCompanyId(orgId)
        val asset = cdvClient.get("/companies/$companyId/assets/$id/metadata", CdvAsset::class.java)
        return Asset(asset.documentGUID, asset.companyId.toString(), asset.fileName)
    }

    override fun updateDocument(orgId: UUID, id: UUID, doc: Document) : Any {
        return updateDocument(orgId, id.toString(), doc)
    }

    override fun updateDocument(orgId: UUID, id: String, doc: Document) : Any {
        val companyId = convertToCompanyId(orgId)
        return cdvClient.put("/companies/$companyId/assets/$id/metadata/es", doc, Json.GENERIC_MAP)
    }

    override fun getDocument(orgId: UUID, id: UUID) : Document {
        return getDocument(orgId, id.toString())
    }

    override fun getDocument(orgId: UUID, id: String) : Document {
        val companyId = convertToCompanyId(orgId)
        return cdvClient.get("/companies/$companyId/assets/$id/metadata/es", Document::class.java)
    }

    private fun convertToCompanyId(orgId: UUID) : Long {
        val org = indexRouteClient.getIndexRoute(orgId)
        return org.name.toLong()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CoreDataVaultAssetRepositoryImpl::class.java)
    }
}
