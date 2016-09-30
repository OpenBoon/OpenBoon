package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.archivist.service.PipelineService;
import com.zorroa.archivist.web.InvalidObjectException;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
public class PipelineController {

    private static final Logger logger = LoggerFactory.getLogger(PipelineController.class);

    @Autowired
    PipelineService pipelineService;

    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.POST)
    public Pipeline create(@Valid @RequestBody PipelineSpecV spec, BindingResult valid) {
        if (valid.hasErrors()) {
            throw new InvalidObjectException("Failed to create pipeline", valid);
        }
        return pipelineService.create(spec);
    }

    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.GET)
    public Pipeline get(@PathVariable String id) {
        if (StringUtils.isNumeric(id)) {
            return pipelineService.get(Integer.parseInt(id));
        }
        else {
            return pipelineService.get(id);
        }
    }

    @RequestMapping(value="/api/v1/pipelines/{id}/_export", method=RequestMethod.GET, produces = {"application/octet-stream"})
    public byte[] export(@PathVariable String id, HttpServletResponse rsp) {

        Pipeline export;
        if (StringUtils.isNumeric(id)) {
            export = pipelineService.get(Integer.parseInt(id));
        }
        else {
            export = pipelineService.get(id);
        }
        export.setId(null);
        rsp.setHeader("Content-disposition", "attachment; filename=\"" + export.getName() + ".json\"");
        return Json.prettyString(export).getBytes();
    }

    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.GET)
    public PagedList<Pipeline> getPaged(@RequestParam(value="page", required=false) Integer page,
                                        @RequestParam(value="count", required=false) Integer count) {
        return pipelineService.getAll(new Pager(page, count));
    }

    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.PUT)
    public Object update(@PathVariable Integer id, @Valid @RequestBody Pipeline spec, BindingResult valid) {
        checkValid(valid);
        return HttpUtils.updated("pipelines", id, pipelineService.update(id, spec), pipelineService.get(id));
    }

    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.DELETE)
    public Object delete(@PathVariable Integer id) {
        return HttpUtils.deleted("pipelines", id, pipelineService.delete(id));
    }

    public static void checkValid(BindingResult valid) {
        if (valid.hasErrors()) {
            throw new InvalidObjectException("Failed to create pipeline", valid);
        }
    }
}
