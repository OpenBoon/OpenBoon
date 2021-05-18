package boonai.archivist.rest

import boonai.archivist.domain.DataSet
import boonai.archivist.domain.DataSetFilter
import boonai.archivist.domain.DataSetSpec
import boonai.archivist.domain.GenericBatchUpdateResponse
import boonai.archivist.domain.UpdateLabelRequest
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.DataSetService
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

@PreAuthorize("hasAuthority('ModelTraining')")
@RestController
class DataSetCotroller(
    val dataSetService: DataSetService
) {

    @ApiOperation("Create a new DataSet")
    @PostMapping(value = ["/api/v3/datasets"])
    fun create(@RequestBody spec: DataSetSpec): DataSet {
        return dataSetService.createDataSet(spec)
    }

    @ApiOperation("Get a DataSet record")
    @GetMapping(value = ["/api/v3/datasets/{id}"])
    fun get(@PathVariable id: UUID): DataSet {
        return dataSetService.getDataSet(id)
    }

    @ApiOperation("Delete a DataSet record")
    @DeleteMapping(value = ["/api/v3/datasets/{id}"])
    fun delete(@PathVariable id: UUID): Any {
        dataSetService.deleteDataSet(dataSetService.getDataSet(id))
        return HttpUtils.deleted("dataSet", id, true)
    }

    @ApiOperation("Rename label")
    @PutMapping("/api/v3/datasets/{id}/labels")
    fun renameLabels(
        @PathVariable id: UUID,
        @RequestBody req: UpdateLabelRequest
    ): GenericBatchUpdateResponse {
        val ds = dataSetService.getDataSet(id)
        return dataSetService.updateLabel(ds, req.label, req.newLabel)
    }

    @ApiOperation("Delete label")
    @DeleteMapping("/api/v3/datasets/{id}/labels")
    fun deleteLabels(
        @PathVariable id: UUID,
        @RequestBody req: UpdateLabelRequest
    ): GenericBatchUpdateResponse {
        val ds = dataSetService.getDataSet(id)
        return dataSetService.updateLabel(ds, req.label, null)
    }

    @ApiOperation("Get the labels for the model")
    @GetMapping(value = ["/api/v3/datasets/{id}/_label_counts"])
    fun getLabels(@PathVariable id: UUID): Map<String, Long> {
        val ds = dataSetService.getDataSet(id)
        return dataSetService.getLabelCounts(ds)
    }

    @PostMapping("/api/v3/datasets/_search")
    fun find(@RequestBody(required = false) filter: DataSetFilter?): KPagedList<DataSet> {
        return dataSetService.find(filter ?: DataSetFilter())
    }

    @PostMapping("/api/v3/datasets/_find_one")
    fun findOne(@RequestBody(required = false) filter: DataSetFilter?): DataSet {
        return dataSetService.findOne(filter ?: DataSetFilter())
    }
}
