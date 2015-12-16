package com.zorroa.archivist.web;

import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.MessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
public class FolderController {

    @Autowired
    FolderService folderService;

    @Autowired
    MessagingService messagingService;

    @RequestMapping(value="/api/v1/folders", method=RequestMethod.POST)
    public Folder create(@RequestBody FolderBuilder builder, HttpSession httpSession) {
        return folderService.create(builder);
    }

    @RequestMapping(value="/api/v1/folders", method=RequestMethod.GET)
    public List<Folder> getAll() {
        return folderService.getAll();
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.GET)
    public Folder get(@PathVariable String id) {
        return folderService.get(id);
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.PUT)
    public Folder update(@RequestBody FolderBuilder builder, @PathVariable String id, HttpSession httpSession) {
        Folder folder = folderService.get(id);
        folderService.update(folder, builder);
        return folderService.get(folder.getId());
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.DELETE)
    public Folder delete(@PathVariable String id, HttpSession httpSession) {
        Folder folder = folderService.get(id);
        if (folderService.delete(folder)) {
            return folder;
        }
        return null;
    }

    @RequestMapping(value="/api/v1/folders/{id}/_children", method=RequestMethod.GET)
    public List<Folder> getChildren(@PathVariable String id) {
        Folder folder = folderService.get(id);
        return folderService.getChildren(folder);
    }
}
