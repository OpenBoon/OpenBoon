package com.zorroa.archivist.web.api;

import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.service.JobExecutorService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created by chambers on 8/24/16.
 */
@RestController
public class TaskController {

    @Autowired
    JobExecutorService jobExecutorService;

    @Autowired
    JobService jobService;

    @Autowired
    TaskDao taskDao;

    @RequestMapping(value = "/api/v1/tasks/{id}/_log", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<InputStreamResource> streamLog(@PathVariable int id) throws ExecutionException, IOException {
        File logFile = taskDao.getLogFilePath(id).toFile();

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/plain"))
                .contentLength(logFile.length())
                .body(new InputStreamResource(new FileInputStream(logFile)));
    }

    @RequestMapping(value = "/api/v1/tasks/{id}/_retry", method = RequestMethod.PUT)
    @ResponseBody
    public void retry(@PathVariable int id) throws ExecutionException, IOException {
        jobExecutorService.retryTask(taskDao.get(id));
    }

    @RequestMapping(value = "/api/v1/tasks/{id}/_skip", method = RequestMethod.PUT)
    @ResponseBody
    public void skip(@PathVariable int id) throws ExecutionException, IOException {
        jobExecutorService.skipTask(taskDao.get(id));
    }

    @RequestMapping(value = "/api/v1/tasks/{id}/_script", method = RequestMethod.GET)
    @ResponseBody
    public String getScript(@PathVariable int id) throws ExecutionException, IOException {
        return Json.prettyString(Json.deserialize(taskDao.getScript(id), Object.class));
    }
}
