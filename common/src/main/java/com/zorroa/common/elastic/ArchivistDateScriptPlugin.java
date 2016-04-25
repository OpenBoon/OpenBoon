package com.zorroa.common.elastic;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptModule;

/**
 * Created by chambers on 4/4/16.
 */
public class ArchivistDateScriptPlugin extends Plugin {
    @Override
    public String name() {
        return "archivistDate";
    }

    @Override
    public String description() {
        return "A script for date aggregations";
    }

    public void onModule(ScriptModule scriptModule) {
        scriptModule.registerScript("archivistDate", ArchivistDateScriptFactory.class);
    }
}
