package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class IndexRouteDaoTests : AbstractTest() {

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Test
    fun testUpdateDefaultIndexRoutes() {
        indexRouteDao.updateDefaultIndexRoutes("http://dog:1234")
    }

}