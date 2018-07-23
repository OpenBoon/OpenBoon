package com.zorroa.analyst.controller

import com.zorroa.analyst.AbstractMvcTest
import com.zorroa.analyst.repository.JobDao
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*
import kotlin.test.assertEquals

@WebAppConfiguration
class JobControllerTests : AbstractMvcTest() {

    @Autowired
    lateinit var jobDao: JobDao

    @Test
    fun testGetAll() {
        val assetId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        for (i in 1..10) {
            val spec = JobSpec("run_some_stuff_v$i",
                    assetId,
                    orgId,
                    listOf("standard"))
            jobDao.create(spec)
        }
        val req = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val kpl = Json.deserialize(req.response.contentAsByteArray, KPagedList::class.java)
        assertEquals(10, kpl.size())
        assertEquals(10, kpl.page.totalCount)
    }
}
