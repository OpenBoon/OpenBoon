package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.sdk.services.AssetId
import com.zorroa.archivist.sdk.services.AssetSpec
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class TaskDaoTests : AbstractTest() {

    @Autowired
    private lateinit var taskDao : TaskDao

    internal lateinit var asset: AssetId

    @Before
    fun init() {
        asset = assetService.create(AssetSpec("dog.png"))
    }

    @Test
    fun testCreate() {
        val spec = TaskSpec(asset.id, null, "test")
        val task = taskDao.create(spec)
        assertEquals(task.assetId, asset.id)
        assertEquals(task.organizationId, asset.organizationId)
        assertEquals(task.pipelineId, null)
    }

    @Test
    fun testGet() {
        val spec = TaskSpec(asset.id, null, "test")
        val task1 = taskDao.create(spec)
        val task2 = taskDao.get(task1.taskId)
        assertEquals(task1.taskId, task2.taskId)
    }
}
