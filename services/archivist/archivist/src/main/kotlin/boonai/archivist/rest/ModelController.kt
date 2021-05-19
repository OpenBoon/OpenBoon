package boonai.archivist.rest

import boonai.archivist.domain.ArgSchema
import boonai.archivist.domain.AutomlSession
import boonai.archivist.domain.AutomlSessionSpec
import boonai.archivist.domain.Job
import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelApplyRequest
import boonai.archivist.domain.ModelApplyResponse
import boonai.archivist.domain.ModelCopyRequest
import boonai.archivist.domain.ModelFilter
import boonai.archivist.domain.ModelPatchRequest
import boonai.archivist.domain.ModelPublishRequest
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelTrainingRequest
import boonai.archivist.domain.ModelType
import boonai.archivist.domain.ModelUpdateRequest
import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.PostTrainAction
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.ArgValidationService
import boonai.archivist.service.AutomlService
import boonai.archivist.service.ModelService
import boonai.archivist.util.HttpUtils
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
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
    val automlService: AutomlService,
    val argValidationService: ArgValidationService
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
    fun train(@PathVariable id: UUID): Job {
        val model = modelService.getModel(id)
        val req = ModelTrainingRequest(postAction = PostTrainAction.APPLY)
        return modelService.trainModel(model, req)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @ApiOperation("Kick off a model training job.")
    @PostMapping(value = ["/api/v4/models/{id}/_train"])
    fun trainV4(@PathVariable id: UUID, @RequestBody request: ModelTrainingRequest): Job {
        val model = modelService.getModel(id)
        return modelService.trainModel(model, request)
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
    fun publish(@PathVariable id: UUID, @RequestBody(required = false) req: ModelPublishRequest?): PipelineMod {
        val model = modelService.getModel(id)
        return modelService.publishModel(model, req ?: ModelPublishRequest())
    }

    @ApiOperation("Set model training arguments")
    @PutMapping("/api/v3/models/{id}/_training_args")
    fun setTrainingArguments(@PathVariable id: UUID, @RequestBody args: Map<String, Any>): Any {
        val model = modelService.getModel(id)
        modelService.setTrainingArgs(model, args)
        return model.trainingArgs
    }

    @ApiOperation("Set model training arguments")
    @PatchMapping("/api/v3/models/{id}/_training_args")
    fun patchTrainingArguments(@PathVariable id: UUID, @RequestBody args: Map<String, Any>): Any {
        val model = modelService.getModel(id)
        modelService.patchTrainingArgs(model, args)
        return model.trainingArgs
    }

    @ApiOperation("Set model training arguments")
    @GetMapping("/api/v3/models/{id}/_training_args")
    fun resolveTrainingArguments(@PathVariable id: UUID): Any {
        val model = modelService.getModel(id)
        return argValidationService.buildArgs(
            modelService.getTrainingArgSchema(model.type), model.trainingArgs
        )
    }

    @ApiOperation("Get model training argument schema")
    @GetMapping("/api/v3/models/_types/{type}/_training_args")
    fun getTrainingArgumentSchema(@PathVariable type: String): ArgSchema {
        return modelService.getTrainingArgSchema(ModelType.valueOf(type.toUpperCase()))
    }

    @ApiOperation("Delete a model")
    @DeleteMapping("/api/v3/models/{id}")
    fun delete(@PathVariable id: UUID): Any {
        val model = modelService.getModel(id)
        modelService.deleteModel(model)
        return HttpUtils.deleted("model", model.id.toString(), true)
    }

    @ApiOperation("Deploy the model and apply to given search.")
    @PostMapping(value = ["/api/v3/models/{id}/_apply"])
    @PreAuthorize("hasAuthority('AssetsImport')")
    fun apply(@PathVariable id: UUID, @RequestBody req: ModelApplyRequest): ModelApplyResponse {
        return modelService.applyModel(modelService.getModel(id), req)
    }

    @ApiOperation("Test the model and apply to given search.")
    @PostMapping("/api/v3/models/{id}/_test")
    @PreAuthorize("hasAuthority('AssetsImport')")
    fun test(@PathVariable id: UUID, @RequestBody req: ModelApplyRequest): ModelApplyResponse {
        return modelService.testModel(modelService.getModel(id), req)
    }

    @ApiOperation("Approve the latest model.")
    @PostMapping("/api/v3/models/{id}/_approve")
    @PreAuthorize("hasAuthority('AssetsImport')")
    fun approve(@PathVariable id: UUID): Any {
        // For now we just copy latest to approved.
        modelService.copyModelTag(
            modelService.getModel(id), ModelCopyRequest("latest", "approved")
        )
        return HttpUtils.status("model", "copyTag", true)
    }

    @ApiOperation("Test the model and apply to given search.")
    @GetMapping("/api/v3/models/{id}/_tags")
    @PreAuthorize("hasAuthority('AssetsImport')")
    fun getVersionTags(@PathVariable id: UUID): Set<String> {
        return modelService.getModelVersions(modelService.getModel(id))
    }

    @ApiOperation("Upload the model zip file.")
    @PostMapping(value = ["/api/v3/models/{id}/_upload"])
    fun upload(@ApiParam("ModelId") @PathVariable id: UUID, req: HttpServletRequest): Any {
        return modelService.publishModelFileUpload(modelService.getModel(id), req.inputStream)
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

    @PutMapping(value = ["/api/v3/models/{id}"])
    fun update(@PathVariable id: UUID, @RequestBody spec: ModelUpdateRequest): Any {
        modelService.updateModel(id, spec)
        return HttpUtils.updated("Model", id, true)
    }

    @PatchMapping(value = ["/api/v3/models/{id}"])
    fun patch(@PathVariable id: UUID, @RequestBody spec: ModelPatchRequest): Any {
        modelService.patchModel(id, spec)
        return HttpUtils.updated("Model", id, true)
    }
}
