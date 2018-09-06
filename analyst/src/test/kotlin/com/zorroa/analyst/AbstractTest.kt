package com.zorroa.analyst

import org.junit.Before
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import javax.sql.DataSource


@Transactional
@TestPropertySource(locations=["classpath:test.properties"])
@RunWith(SpringRunner::class)
@SpringBootTest
open abstract class AbstractTest {

    protected val logger = LoggerFactory.getLogger(javaClass)

    protected lateinit var jdbc : JdbcTemplate

    @Autowired
    fun setDatasource(dataSource: DataSource) {
        this.jdbc = JdbcTemplate(dataSource)
    }
}

@Transactional
@TestPropertySource(locations=["classpath:test.properties"])
@RunWith(SpringRunner::class)
@SpringBootTest
open abstract class AbstractMvcTest {

    protected val logger = LoggerFactory.getLogger(javaClass)

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var wac: WebApplicationContext

    @Before
    fun setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
    }
}
