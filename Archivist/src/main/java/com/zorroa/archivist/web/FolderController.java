package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderBuilder;
import com.zorroa.archivist.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FolderController {

    @Autowired
    FolderService folderService;

    @RequestMapping(value="/api/v1/folders", method=RequestMethod.POST)
    public Folder create(@RequestBody FolderBuilder builder) {
        return folderService.create(builder);
    }

    @RequestMapping(value="/api/v1/folders", method=RequestMethod.GET)
    public List<Folder> getAll(@RequestParam int userId) {
        return folderService.getAll(userId);
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.GET)
    public Folder get(@PathVariable String id) {
        return folderService.get(id);
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.PUT)
    public Folder update(@RequestBody FolderBuilder builder, @PathVariable String id) {
        Folder folder = folderService.get(id);
        folderService.update(folder, builder);
        return folderService.get(folder.getId());
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.DELETE)
    public boolean delete(@PathVariable String id) {
        Folder folder = folderService.get(id);
        return folderService.delete(folder);
    }

    @RequestMapping(value="/api/v1/folders/{id}/_children", method=RequestMethod.GET)
    public List<Folder> getChildren(@PathVariable String id) {
        Folder folder = folderService.get(id);
        return folderService.getChildren(folder);
    }
}
