package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Taxonomy
import com.zorroa.archivist.domain.TaxonomySpec
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.service.TaxonomyService
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@Timed
class TaxonomyController @Autowired constructor(
        private val taxonomyService: TaxonomyService,
        private val folderService: FolderService
){

    @ResponseBody
    @PostMapping(value = ["/api/v1/taxonomy"])
    fun create(@RequestBody tspec: TaxonomySpec): Any {
        return taxonomyService.create(tspec)
    }

    @ResponseBody
    @GetMapping(value = ["/api/v1/taxonomy/{id}"])
    operator fun get(@PathVariable id: UUID): Taxonomy {
        return taxonomyService.get(id)
    }

    @ResponseBody
    @GetMapping(value = ["/api/v1/taxonomy/_folder/{id}"])
    fun getByFolder(@PathVariable id: UUID): Taxonomy {
        val folder = folderService.get(id)
        return taxonomyService.get(folder)
    }

    @ResponseBody
    @GetMapping(value = ["/api/v1/taxonomy/{id}/_retag"])
    fun execute(@PathVariable id: UUID): Any {
        val tax = taxonomyService.get(id)
        taxonomyService.tagTaxonomyAsync(tax, null, true)
        return HttpUtils.status("taxonomy", "retag", true)
    }

    @ResponseBody
    @DeleteMapping(value = ["/api/v1/taxonomy/{id}"])
    fun delete(@PathVariable id: UUID): Any {
        val tax = taxonomyService.get(id)
        return HttpUtils.deleted("taxonomy", id, taxonomyService.delete(tax, true))
    }
}
