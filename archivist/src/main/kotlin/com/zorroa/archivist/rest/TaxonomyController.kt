package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Taxonomy
import com.zorroa.archivist.domain.TaxonomySpec
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.service.TaxonomyService
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Timed
@Api(tags = ["Taxonomy"], description = "Operations for interacting with Taxonomies.")
class TaxonomyController @Autowired constructor(
    private val taxonomyService: TaxonomyService,
    private val folderService: FolderService
) {

    @ApiOperation("Create a Taxonomy.")
    @ResponseBody
    @PostMapping(value = ["/api/v1/taxonomy"])
    fun create(@ApiParam("Taxonomy to create.") @RequestBody tspec: TaxonomySpec): Any {
        return taxonomyService.create(tspec)
    }

    @ApiOperation("Get a Taxonomy.")
    @ResponseBody
    @GetMapping(value = ["/api/v1/taxonomy/{id}"])
    operator fun get(@ApiParam("UUID of the Taxonomy.") @PathVariable id: UUID): Taxonomy {
        return taxonomyService.get(id)
    }

    @ApiOperation("Get a folder in a Taxonomy.")
    @ResponseBody
    @GetMapping(value = ["/api/v1/taxonomy/_folder/{id}"])
    fun getByFolder(@ApiParam("UUID of the Taxonomy folder.") @PathVariable id: UUID): Taxonomy {
        val folder = folderService.get(id)
        return taxonomyService.get(folder)
    }

    @ApiOperation("Re-tag a Taxonomy.",
        notes = "Starts a background task that retags a Taxonomy.")
    @ResponseBody
    @GetMapping(value = ["/api/v1/taxonomy/{id}/_retag"])
    fun execute(@ApiParam("UUID of the Taxonomy.") @PathVariable id: UUID): Any {
        val tax = taxonomyService.get(id)
        taxonomyService.tagTaxonomyAsync(tax, null, true)
        return HttpUtils.status("taxonomy", "retag", true)
    }

    @ApiOperation("Delete a Taxonomy.")
    @ResponseBody
    @DeleteMapping(value = ["/api/v1/taxonomy/{id}"])
    fun delete(@ApiParam("UUID of the Taxonomy.") @PathVariable id: UUID): Any {
        val tax = taxonomyService.get(id)
        return HttpUtils.deleted("taxonomy", id, taxonomyService.delete(tax, true))
    }
}
