package com.zorroa.auth.security

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class SecurityUtilTests {

    @Test
    fun loadServiceKeyFromFile() {
        val filePath = "src/test/resources/key.json"
        val apikey = loadServiceKey(filePath)

        assertEquals(
            UUID.fromString("50550AAC-6C5A-41CD-B779-2821BB5B535F"),
            apikey.projectId
        )

        assertEquals(
            UUID.fromString("76094317-D968-43A8-B9DC-D0680A899AD7"),
            apikey.keyId
        )

        assertEquals(
            "pcekjDV_ipSMXAaBqqtq6Jwy5FAMnjehUQrMEhbG8W01giVqVLfEN9FdMIvzu0rb",
            apikey.sharedKey
        )
    }

    @Test
    fun loadServiceKeyFromBase64() {
        val base64 = "ewogICJuYW1lIjogImFkbWluLWtleSIsCiAgInByb2plY3RJZCI6ICI1MDU1ME" +
            "FBQy02QzVBLTQxQ0QtQjc3OS0yODIxQkI1QjUzNUYiLAogICJrZXlJZCI6ICI3NjA5NDMxNy" +
            "1EOTY4LTQzQTgtQjlEQy1EMDY4MEE4OTlBRDciLAogICJzaGFyZWRLZXkiOiAicGNla2pEVl9" +
            "pcFNNWEFhQnFxdHE2Snd5NUZBTW5qZWhVUXJNRWhiRzhXMDFnaVZxVkxmRU45RmRNSXZ6dTByY" +
            "iIsCiAgInBlcm1pc3Npb25zIjogWyJTdXBlckFkbWluIl0KfQoK"
        val apikey = loadServiceKey(base64)

        assertEquals(
            UUID.fromString("50550AAC-6C5A-41CD-B779-2821BB5B535F"),
            apikey.projectId
        )

        assertEquals(
            UUID.fromString("76094317-D968-43A8-B9DC-D0680A899AD7"),
            apikey.keyId
        )

        assertEquals(
            "pcekjDV_ipSMXAaBqqtq6Jwy5FAMnjehUQrMEhbG8W01giVqVLfEN9FdMIvzu0rb",
            apikey.sharedKey
        )
    }
}