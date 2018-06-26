package com.zorroa.irm.studio

import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional


@Transactional
@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
open abstract class AbstractTest {

    protected val logger = LoggerFactory.getLogger(javaClass)

}
