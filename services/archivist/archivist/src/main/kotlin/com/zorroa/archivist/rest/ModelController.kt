package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.AutomlSession
import com.zorroa.archivist.domain.AutomlSessionSpec
import com.zorroa.archivist.domain.GenericBatchUpdateResponse
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelApplyRequest
import com.zorroa.archivist.domain.ModelApplyResponse
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelTrainingArgs
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.UpdateLabelRequest
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.service.AutomlService
import com.zorroa.archivist.service.ModelService
import com.zorroa.archivist.util.HttpUtils
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@RestController
class ModelController(
    val modelService: ModelService,
    val automlService: AutomlService
) {

    @PreAuthorize("hasAuthority('AssetsImport')")
    @ApiOperation("Create a new Model")
    @PostMapping(value = ["/api/v3/models"])
    fun create(@RequestBody spec: ModelSpec): Model {
        return modelService.createModel(spec)
    }

    @ApiOperation("Get a Model record")
    @GetMapping(value = ["/api/v3/models/{id}"])
    fun get(@PathVariable id: UUID): Model {
        return modelService.getModel(id)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @ApiOperation("Kick off a model training job.")
    @PostMapping(value = ["/api/v3/models/{id}/_train"])
    fun train(@PathVariable id: UUID, @RequestBody args: ModelTrainingArgs): Job {
        val model = modelService.getModel(id)
        return modelService.trainModel(model, args)
    }

    @ApiOperation("Get Information about a model type.")
    @GetMapping(value = ["/api/v3/models/_types/{name}"])
    fun getType(@PathVariable name: String): Map<String, Any> {
        return ModelType.valueOf(name).asMap()
    }

    @ApiOperation("Get Information about all model types.")
    @GetMapping(value = ["/api/v3/models/_types"])
    fun getTypes(): Any {
        return ModelType.values().map { it.asMap() }
    }

    @ApiOperation("Search for Models.")
    @PostMapping("/api/v3/models/_search")
    fun find(@RequestBody(required = false) filter: ModelFilter?): KPagedList<Model> {
        return modelService.find(filter ?: ModelFilter())
    }

    @ApiOperation("Find a single Model")
    @PostMapping("/api/v3/models/_find_one")
    fun findOne(@RequestBody(required = false) filter: ModelFilter?): Model {
        return modelService.findOne(filter ?: ModelFilter())
    }

    @ApiOperation("Publish a model as a PipelineMod")
    @PostMapping("/api/v3/models/{id}/_publish")
    @PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','SystemManage')")
    fun publish(@PathVariable id: UUID): PipelineMod {
        val model = modelService.getModel(id)
        return modelService.publishModel(model)
    }

    @ApiOperation("Delete a model")
    @DeleteMapping("/api/v3/models/{id}")
    fun delete(@PathVariable id: UUID): Any {
        val model = modelService.getModel(id)
        modelService.deleteModel(model)
        return HttpUtils.deleted("model", model.id.toString(), true)
    }

    @ApiOperation("Deploy the model and apply to given search.")
    @PostMapping("/api/v3/models/{id}/_deploy")
    @PreAuthorize("hasAuthority('AssetsImport')")
    fun apply(@PathVariable id: UUID, @RequestBody req: ModelApplyRequest): ModelApplyResponse {
        return modelService.deployModel(modelService.getModel(id), req)
    }

    @ApiOperation("Get the labels for the model")
    @GetMapping(value = ["/api/v3/models/{id}/_label_counts"])
    fun getLabels(@ApiParam("ModelId") @PathVariable id: UUID): Map<String, Long> {
        return modelService.getLabelCounts(modelService.getModel(id))
    }

    @ApiOperation("Upload the model zip file.")
    @PostMapping(value = ["/api/v3/models/{id}/_upload"])
    fun upload(@ApiParam("ModelId") @PathVariable id: UUID, req: HttpServletRequest): Any {
        return modelService.acceptModelFileUpload(modelService.getModel(id), req.inputStream)
    }

    @ApiOperation("Rename label")
    @PutMapping("/api/v3/models/{id}/labels")
    fun renameLabels(
        @ApiParam("ModelId") @PathVariable id: UUID,
        @RequestBody req: UpdateLabelRequest
    ): GenericBatchUpdateResponse {
        val model = modelService.getModel(id)
        return modelService.updateLabel(model, req.label, req.newLabel)
    }

    @ApiOperation("Delete label")
    @DeleteMapping("/api/v3/models/{id}/labels")
    fun deleteLabels(
        @ApiParam("ModelId") @PathVariable id: UUID,
        @RequestBody req: UpdateLabelRequest
    ): GenericBatchUpdateResponse {
        val model = modelService.getModel(id)
        return modelService.updateLabel(model, req.label, null)
    }

    @PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','SystemManage')")
    @ApiOperation("Create an autoML session.")
    @PostMapping("/api/v3/models/{id}/_automl")
    fun createAutomlSession(
        @ApiParam("ModelId") @PathVariable id: UUID,
        @RequestBody spec: AutomlSessionSpec
    ): AutomlSession {
        val model = modelService.getModel(id)
        return automlService.createSession(model, spec)
    }
}
