package com.zorroa.analyst

import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional


@Transactional
@TestPropertySource(locations=["classpath:test.properties"])
@RunWith(SpringRunner::class)
@SpringBootTest
open abstract class AbstractTest {

    protected val logger = LoggerFactory.getLogger(javaClass)

}
