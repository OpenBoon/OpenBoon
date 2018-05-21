package com.zorroa.archivist.repository

import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.domain.AnalystState
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import org.springframework.stereotype.Repository

interface AnalystDao {

    fun getRunningTaskIds(): List<Int>

    fun register(spec: AnalystSpec): String

    fun setState(id: String, state: AnalystState)

    fun get(id: String): Analyst

    fun count(): Long

    fun delete(a: Analyst): Boolean

    fun getAll(paging: Pager): PagedList<Analyst>

    fun getExpired(limit: Int, duration: Long): List<Analyst>

    /**
     * Return a list of analysts that are in the UP state but are not updating
     * their data at regular intervals.
     *
     * @param limit
     * @param duration
     * @return
     */
    fun getUnresponsive(limit: Int, duration: Long): List<Analyst>

    fun getActive(paging: Pager): List<Analyst>

    fun getReady(paging: Pager): List<Analyst>
}

@Repository
class AnalystDaoImpl : AnalystDao {

    override fun getRunningTaskIds(): List<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun register(spec: AnalystSpec): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setState(id: String, state: AnalystState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun get(id: String): Analyst {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun count(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(a: Analyst): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAll(paging: Pager): PagedList<Analyst> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getExpired(limit: Int, duration: Long): List<Analyst> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnresponsive(limit: Int, duration: Long): List<Analyst> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getActive(paging: Pager): List<Analyst> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReady(paging: Pager): List<Analyst> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}



