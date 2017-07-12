package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Command;
import com.zorroa.archivist.service.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by chambers on 4/24/17.
 */
@RestController
public class CommandController {

    @Autowired
    CommandService commandService;

    @RequestMapping(value = "/api/v1/commands/{id:\\d+}", method = RequestMethod.GET)
    public Command get(@PathVariable int id) {
        return commandService.get(id);
    }

    @RequestMapping(value = "/api/v1/commands/{id:\\d+}/_cancel", method = RequestMethod.PUT)
    public Object cancel(@PathVariable int id) {
        Command cmd = commandService.get(id);
        boolean canceled = commandService.cancel(cmd);
        return HttpUtils.status("command", "cancel", canceled);
    }

    @RequestMapping(value = "/api/v1/commands", method = RequestMethod.GET)
    public List<Command> getPending() {
        return commandService.getPendingByUser();
    }
}
