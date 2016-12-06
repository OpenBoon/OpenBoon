package com.zorroa.common.elastic;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.script.ScriptModule;

import java.util.Map;

/**
 * Created by chambers on 12/6/16.
 */
public class HammingDistancePlugin extends Plugin {
    @Override
    public String name() {
        return "hammingDistance";
    }

    @Override
    public String description() {
        return "Example from StackOverflow similar-image-search-by-phash-distance-in-elasticsearch";
    }

    public void onModule(ScriptModule scriptModule) {
        scriptModule.registerScript("hammingDistance", HammingDistanceFactory.class);
    }

    public static class HammingDistanceFactory implements NativeScriptFactory {
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            return new HammingDistanceScript(params);
        }

        @Override
        public boolean needsScores() {
            return false;
        }
    }
}
