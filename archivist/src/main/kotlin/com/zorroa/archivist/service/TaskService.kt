package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.repository.TaskDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


interface TaskService {
    fun create(spec: TaskSpec) : Task
    fun get(id: UUID) : Task
}

@Service
@Transactional
class TaskServiceInternalImpl @Autowired constructor (
        val taskDao: TaskDao,
        val tx: TransactionEventManager) : TaskService {

    override fun create(spec: TaskSpec) : Task {
        return taskDao.create(spec)
    }

    override fun get(id: UUID) : Task {
        return taskDao.get(id)
    }
}
