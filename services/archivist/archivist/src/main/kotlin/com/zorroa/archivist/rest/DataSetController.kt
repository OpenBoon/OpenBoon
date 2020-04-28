package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.DataSet
import com.zorroa.archivist.domain.DataSetFilter
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.service.DataSetService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DataSetController(
    val datasetService: DataSetService
) {

    @ApiOperation("Create a DataSet")
    @PostMapping("/api/v1/data-sets")
    fun create(@ApiParam("Create a new DataSet") @RequestBody spec: DataSetSpec): DataSet {
        return datasetService.create(spec)
    }

    @ApiOperation("Get a DataSet")
    @GetMapping("/api/v1/data-sets/{id}")
    fun get(@ApiParam("DataSet ID") @PathVariable id: UUID): DataSet {
        return datasetService.get(id)
    }

    @ApiOperation("Search for DataSets.")
    @PostMapping("/api/v3/data-sets/_search")
    fun find(@RequestBody(required = false) filter: DataSetFilter?): KPagedList<DataSet> {
        return datasetService.find(filter ?: DataSetFilter())
    }

    @ApiOperation("Find a single DataSet")
    @PostMapping("/api/v3/data-sets/_find_one")
    fun findOne(@RequestBody(required = false) filter: DataSetFilter?): DataSet {
        return datasetService.findOne(filter ?: DataSetFilter())
    }

    @ApiOperation("Find a single DataSet")
    @GetMapping("/api/v3/data-sets/{id}/_label_counts")
    fun getLabelCounts(@ApiParam("DataSet ID") @PathVariable id: UUID): Map<String, Long> {
        return datasetService.getLabelCounts(datasetService.get(id))
    }

    @ApiOperation("Find a single DataSet")
    @PostMapping("/api/v3/data-sets/{id}/_train_model")
    fun trainModel(
        @ApiParam("DataSet ID") @PathVariable id: UUID,
        @RequestBody(required = true) spec: ModelSpec
    ): Job {
        return datasetService.trainModel(datasetService.get(id), spec)
    }
}
