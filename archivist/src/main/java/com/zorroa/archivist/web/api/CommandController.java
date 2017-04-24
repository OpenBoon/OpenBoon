package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.Command;
import com.zorroa.archivist.service.CommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by chambers on 4/24/17.
 */
@RestController
public class CommandController {

    private static final Logger logger = LoggerFactory.getLogger(CommandController.class);

    @Autowired
    CommandService commandService;

    @RequestMapping(value="/api/v1/commands/{id:\\d+}", method= RequestMethod.GET)
    public Command get(@PathVariable int id) {
        return commandService.get(id);
    }
}
