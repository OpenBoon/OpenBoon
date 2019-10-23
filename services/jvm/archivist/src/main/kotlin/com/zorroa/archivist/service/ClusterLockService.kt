package com.zorroa.archivist.service

import com.zorroa.archivist.domain.ClusterLockSpec
import org.springframework.stereotype.Service

interface ClusterLockService {

}

interface ClusterLockExecutor {


    /**
     * Run the given code inline with the current thread.
     *
     * @param spec A ClusterLock spec
     * @param body The code to execute
     */
    fun <T : Any?> inline(spec: ClusterLockSpec, body: () -> T?): T?

}

@Service
class CusterLockExecutorImpl : ClusterLockExecutor {

    override fun <T> inline(spec: ClusterLockSpec, body: () -> T?): T? {
        return body()
    }
}