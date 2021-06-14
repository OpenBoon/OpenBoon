package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Dataset
import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelType
import boonai.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.FileInputStream
import java.nio.file.Paths

class ModelDeployServiceTests : AbstractTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var modelDeployService: ModelDeployService

    val testSearch =
        """{"query": {"term": { "source.filename": "large-brown-cat.jpg"} } }"""

    fun create(name: String = "test", type: ModelType = ModelType.TF_CLASSIFIER, ds: Dataset? = null): Model {
        val mspec = ModelSpec(
            name,
            type,
            datasetId = ds?.id,
            applySearch = Json.Mapper.readValue(testSearch, Json.GENERIC_MAP)
        )
        return modelService.createModel(mspec)
    }

    @Test
    fun testAcceptModelFileUploadAndList() {
        val model = create(type = ModelType.TF_SAVED_MODEL)
        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )

        modelDeployService.deployUploadedModel(model, FileInputStream(mfp.toFile()))
    }
}
