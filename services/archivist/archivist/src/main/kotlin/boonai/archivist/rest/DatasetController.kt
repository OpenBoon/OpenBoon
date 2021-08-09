package boonai.archivist.rest

import boonai.archivist.domain.Dataset
import boonai.archivist.domain.DatasetFilter
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.DatasetUpdate
import boonai.archivist.domain.GenericBatchUpdateResponse
import boonai.archivist.domain.UpdateLabelRequest
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.BoonLibService
import boonai.archivist.service.DatasetService
import boonai.archivist.util.HttpUtils
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','ModelTraining')")
@RestController
class DatasetController(
    val datasetService: DatasetService,
    val boonLibService: BoonLibService,
) {

    @ApiOperation("Create a new Dataset")
    @PostMapping(value = ["/api/v3/datasets"])
    fun create(@RequestBody spec: DatasetSpec): Dataset {
        return datasetService.createDataset(spec)
    }

    @ApiOperation("Get a Dataset record")
    @GetMapping(value = ["/api/v3/datasets/{id}"])
    fun get(@PathVariable id: UUID): Dataset {
        return datasetService.getDataset(id)
    }

    @ApiOperation("Delete a Dataset record")
    @DeleteMapping(value = ["/api/v3/datasets/{id}"])
    fun delete(@PathVariable id: UUID): Any {
        datasetService.deleteDataset(datasetService.getDataset(id))
        return HttpUtils.deleted("dataset", id, true)
    }

    @ApiOperation("Update a Dataset record")
    @PutMapping(value = ["/api/v3/datasets/{id}"])
    fun put(@PathVariable id: UUID, @RequestBody spec: DatasetUpdate): Any {
        val ds = datasetService.getDataset(id)
        datasetService.updateDataset(ds, spec)
        return HttpUtils.updated("dataset", id, true)
    }

    @ApiOperation("Rename label")
    @PutMapping("/api/v3/datasets/{id}/labels")
    fun renameLabels(
        @PathVariable id: UUID,
        @RequestBody req: UpdateLabelRequest
    ): GenericBatchUpdateResponse {
        val ds = datasetService.getDataset(id)
        return datasetService.updateLabel(ds, req.label, req.newLabel)
    }

    @ApiOperation("Delete label")
    @DeleteMapping("/api/v3/datasets/{id}/labels")
    fun deleteLabels(
        @PathVariable id: UUID,
        @RequestBody req: UpdateLabelRequest
    ): GenericBatchUpdateResponse {
        val ds = datasetService.getDataset(id)
        return datasetService.updateLabel(ds, req.label, null)
    }

    @ApiOperation("Get the labels for the model")
    @GetMapping(value = ["/api/v3/datasets/{id}/_label_counts"])
    fun getLabels(@PathVariable id: UUID): Map<String, Long> {
        val ds = datasetService.getDataset(id)
        return datasetService.getLabelCounts(ds)
    }

    @ApiOperation("Get the labels for the model")
    @GetMapping(value = ["/api/v4/datasets/{id}/_label_counts"])
    fun getLabelsV4(@PathVariable id: UUID): Any {
        val ds = datasetService.getDataset(id)
        return datasetService.getLabelCountsV4(ds)
    }

    @PostMapping("/api/v3/datasets/_search")
    fun find(@RequestBody(required = false) filter: DatasetFilter?): KPagedList<Dataset> {
        return datasetService.find(filter ?: DatasetFilter())
    }

    @PostMapping("/api/v3/datasets/_find_one")
    fun findOne(@RequestBody(required = false) filter: DatasetFilter?): Dataset {
        return datasetService.findOne(filter ?: DatasetFilter())
    }

    @ApiOperation("Get Information about all dataset types.")
    @GetMapping(value = ["/api/v3/datasets/_types"])
    fun getTypes(): Any {
        return DatasetType.values().map { it.asMap() }
    }
}
