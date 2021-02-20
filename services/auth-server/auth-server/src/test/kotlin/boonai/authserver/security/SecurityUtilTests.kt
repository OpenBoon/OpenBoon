package boonai.authserver.security

import java.util.UUID
import junit.framework.Assert.assertEquals
import org.junit.Test

class SecurityUtilTests {

    @Test
    fun hasAnyPermission() {
    }

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
            apikey.id
        )

        assertEquals(
            "pcekjDV_ipSMXAaBqqtq6Jwy5FAMnjehUQrMEhbG8W01giVqVLfEN9FdMIvzu0rb",
            apikey.secretKey
        )
    }

    @Test
    fun loadServiceKeyFromBase64() {
        val base64 = "ewogICJuYW1lIjogImFkbWluLWtleSIsCiAgInByb2plY3RJZCI6ICI1MDU1MEFBQy02" +
            "QzVBLTQxQ0QtQjc3OS0yODIxQkI1QjUzNUYiLAogICJpZCI6ICI3NjA5NDMxNy1EOTY4LTQzQTgtQj" +
            "lEQy1EMDY4MEE4OTlBRDciLAogICJhY2Nlc3NLZXkiOiAiQkZCQUNCNTUwNjczNDBGMjg0Qzk1OURD" +
            "NTU5NUVCQTMiLAogICJzZWNyZXRLZXkiOiAicGNla2pEVl9pcFNNWEFhQnFxdHE2Snd5NUZBTW5qZWh" +
            "VUXJNRWhiRzhXMDFnaVZxVkxmRU45RmRNSXZ6dTByYiIsCiAgInBlcm1pc3Npb25zIjogW10KfQ=="

        val apikey = loadServiceKey(base64)

        assertEquals(
            UUID.fromString("50550AAC-6C5A-41CD-B779-2821BB5B535F"),
            apikey.projectId
        )

        assertEquals(
            UUID.fromString("76094317-D968-43A8-B9DC-D0680A899AD7"),
            apikey.id
        )

        assertEquals(
            "pcekjDV_ipSMXAaBqqtq6Jwy5FAMnjehUQrMEhbG8W01giVqVLfEN9FdMIvzu0rb",
            apikey.secretKey
        )
    }
}
