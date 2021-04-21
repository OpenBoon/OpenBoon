package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.ArgRequiredException
import boonai.archivist.domain.ArgSchema
import boonai.archivist.domain.Argument
import boonai.archivist.domain.ArgTypeException
import boonai.archivist.domain.ArgUnknownException
import boonai.archivist.domain.ArgumentType
import boonai.common.util.Json
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class ArgValidationServiceTests : AbstractTest() {

    @Autowired
    lateinit var argValidationService: ArgValidationService

    @Test
    fun testLoadArgSchema() {
        val schema = argValidationService.getArgSchema("models/TF_CLASSIFIER")
        assertTrue("epochs" in schema.args.keys)
    }

    @Test(expected = ArgTypeException::class)
    fun testValidateIntTypeFailure() {
        val schema = ArgSchema("n_clusters" to Argument(ArgumentType.Integer, 4))
        argValidationService.validateArgs(schema, mutableMapOf("n_clusters" to "dog"))
    }

    @Test(expected = ArgTypeException::class)
    fun testValidateStringTypeFailure() {
        val schema = ArgSchema("n_clusters" to Argument(ArgumentType.String))
        argValidationService.validateArgs(schema, mutableMapOf("n_clusters" to 4))
    }

    @Test(expected = ArgTypeException::class)
    fun testValidateUrlTypeFailure() {
        val schema = ArgSchema("url" to Argument(ArgumentType.Url))
        argValidationService.validateArgs(schema, mutableMapOf("url" to "nbc://foo"))
    }

    @Test(expected = ArgTypeException::class)
    fun testValidateUrl() {
        val schema = ArgSchema("url" to Argument(ArgumentType.Url))
        argValidationService.validateArgs(schema, mutableMapOf("url" to "https://foo"))
    }

    @Test(expected = ArgRequiredException::class)
    fun testRequiredError() {
        val schema = ArgSchema("n_clusters" to Argument(ArgumentType.Integer, required = true))
        argValidationService.validateArgs(schema, mutableMapOf())
    }

    @Test(expected = ArgRequiredException::class)
    fun testNestedObjectRequiredError() {
        val schema = ArgSchema(
            "cat" to Argument(ArgumentType.Object).subArgs(
                "hair_color" to Argument(ArgumentType.String, required = true)
            )
        )
        argValidationService.validateArgs(schema, mutableMapOf())
    }

    @Test(expected = ArgUnknownException::class)
    fun testInvalidArgName() {
        val schema = ArgSchema("n_clusters" to Argument(ArgumentType.Integer))
        argValidationService.validateArgs(schema, mutableMapOf("dog" to "cat"))
    }

    @Test(expected = ArgUnknownException::class)
    fun testNestedObjectInvalidArgName() {
        val schema = ArgSchema(
            "cat" to Argument(ArgumentType.Object).subArgs(
                "hair_color" to Argument(ArgumentType.String)
            )
        )
        argValidationService.validateArgs(
            schema,
            mutableMapOf("cat" to mutableMapOf<String, Any>("paws" to 2))
        )
    }

    @Test(expected = ArgUnknownException::class)
    fun testSimpleUnknownArg() {
        val schema = ArgSchema("n_clusters" to Argument(ArgumentType.Integer))
        argValidationService.validateArgs(
            schema,
            mutableMapOf("cat" to mutableMapOf<String, Any>("paws" to 2))
        )
    }

    @Test
    fun testBuildArgs() {
        val schema = ArgSchema(
            "n_clusters" to Argument(ArgumentType.Integer, 4),
            "cat" to Argument(ArgumentType.Object).subArgs(
                "hair" to Argument(ArgumentType.String, "brown"),
                "eyes" to Argument(ArgumentType.String)
            )
        )
        val args = argValidationService.buildArgs(
            schema,
            mapOf("cat" to mapOf("hair" to "blue", "eyes" to "brown"))
        )

        val expecting =
            """{"n_clusters":4,"cat":{"hair":"blue","eyes":"brown"}}"""
        assertEquals(expecting, Json.serializeToString(args))
    }
}
