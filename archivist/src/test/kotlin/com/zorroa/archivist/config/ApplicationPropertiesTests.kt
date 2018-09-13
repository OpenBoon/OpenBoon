package com.zorroa.archivist.config

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import kotlin.test.assertEquals


class ApplicationPropertiesTests : AbstractTest() {

    @Test
    fun parseToMap() {
        val result = properties.parseToMap("archivist.security.saml.permissions.map")
        assertEquals(2, result.size)
    }

}