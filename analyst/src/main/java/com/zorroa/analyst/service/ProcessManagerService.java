package com.zorroa.analyst.service;

import com.zorroa.sdk.zps.ZpsScript;

import java.util.Map;

/**
 * Created by chambers on 2/8/16.
 */
public interface ProcessManagerService {

    void execute(ZpsScript script);

    void execute(ZpsScript script, Map<String,Object> args);

    void queueExecute(ZpsScript script);

}
