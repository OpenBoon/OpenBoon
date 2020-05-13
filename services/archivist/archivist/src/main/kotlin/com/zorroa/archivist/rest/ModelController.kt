package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelTrainingArgs
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.service.ModelService
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ModelController(
    val modelService: ModelService
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
    @GetMapping(value = ["/api/v3/models/_type/{name}"])
    fun getType(@PathVariable name: String): Map<String, Any> {
        return ModelType.valueOf(name).asMap()
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
}
