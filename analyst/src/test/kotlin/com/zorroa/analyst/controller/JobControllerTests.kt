package com.zorroa.analyst.controller

import com.zorroa.analyst.AbstractMvcTest
import com.zorroa.analyst.repository.JobDao
import com.zorroa.common.domain.*
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
    fun getCreate() {
        val orgId = UUID.randomUUID()
        val spec = JobSpec("test_job",
                PipelineType.Import,
                orgId,
                ZpsScript("foo", over=mutableListOf(Document(UUID.randomUUID().toString()))),
                env=mutableMapOf("foo" to "bar"))

        val req = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val job = Json.deserialize(req.response.contentAsByteArray, Job::class.java)
        assertEquals(spec.name, job.name)
        assertEquals(spec.type, job.type)
        assertEquals(spec.organizationId, job.organizationId)
        assertEquals(spec.env, job.env)
    }

    @Test
    fun testGetAll() {
        val orgId = UUID.randomUUID()
        for (i in 1..10) {
            val spec = JobSpec("run_some_stuff_v$i",
                    PipelineType.Import,
                    orgId,
                    ZpsScript("foo"))
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
