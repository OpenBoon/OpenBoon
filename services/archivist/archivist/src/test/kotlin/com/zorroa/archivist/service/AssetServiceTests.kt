package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetLinks
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.domain.LinkType
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.UpdateAssetRequest
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssetServiceTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    override fun requiresFieldSets(): Boolean {
        return true
    }

    @Before
    fun init() {
        addTestAssets("set04/standard")
    }

    fun testBatchCreateOrReplace() {

    }

    @Test
    fun testGet() {

    }


}
