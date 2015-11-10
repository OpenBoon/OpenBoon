package com.zorroa.archivist.web;

import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.service.UserService;
import com.zorroa.archivist.sdk.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class FolderController {

    @Autowired
    FolderService folderService;

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

    private void sendFolderToRoom(MessageType type, Folder folder, HttpSession httpSession) {
        Session session = userService.getActiveSession();
        Room room = roomService.getActiveRoom(session);
        String folderJSON = new String(Json.serialize(folder), StandardCharsets.UTF_8);
        roomService.sendToRoom(room, new Message(type, folderJSON));
    }

    @RequestMapping(value="/api/v1/folders", method=RequestMethod.POST)
    public Folder create(@RequestBody FolderBuilder builder, HttpSession httpSession) {
        Folder folder = folderService.create(builder);
        sendFolderToRoom(MessageType.FOLDER_CREATE, folder, httpSession);
        return folder;
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
    public Folder update(@RequestBody FolderBuilder builder, @PathVariable String id, HttpSession httpSession) {
        Folder folder = folderService.get(id);
        folderService.update(folder, builder);
        folder = folderService.get(folder.getId());
        sendFolderToRoom(MessageType.FOLDER_UPDATE, folder, httpSession);
        return folder;
    }

    @RequestMapping(value="/api/v1/folders/{id}", method=RequestMethod.DELETE)
    public Folder delete(@PathVariable String id, HttpSession httpSession) {
        Folder folder = folderService.get(id);
        sendFolderToRoom(MessageType.FOLDER_DELETE, folder, httpSession);
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
