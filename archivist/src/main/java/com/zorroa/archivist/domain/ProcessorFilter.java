package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.repository.DaoFilter;
import com.zorroa.sdk.processor.ProcessorType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by chambers on 8/17/16.
 */
public class ProcessorFilter extends DaoFilter {
    private static final Map<String,String> sortMap = ImmutableMap.<String, String>builder()
            .put("id", "processor.pk_processor")
            .put("name", "processor.str_name")
            .put("type", "processor.int_type")
            .put("shortName", "processor.str_short_name")
            .put("module", "processor.str_module")
            .put("description", "processor.str_description")
            .put("pluginId", "plugin.pk_plugin")
            .put("pluginName", "plugin.str_name")
            .put("pluginLanguage", "plugin.str_lang")
            .build();

    private List<String> modules;
    private List<String> names;
    private List<String> shortNames;
    private Collection<ProcessorType> types;
    private List<UUID> plugins;

    public List<String> getModules() {
        return modules;
    }

    public ProcessorFilter setModules(List<String> modules) {
        this.modules = modules;
        return this;
    }

    public List<String> getNames() {
        return names;
    }

    public ProcessorFilter setNames(List<String> names) {
        this.names = names;
        return this;
    }

    public List<String> getShortNames() {
        return shortNames;
    }

    public ProcessorFilter setShortNames(List<String> shortNames) {
        this.shortNames = shortNames;
        return this;
    }

    public Collection<ProcessorType> getTypes() {
        return types;
    }

    public ProcessorFilter setTypes(Collection<ProcessorType> types) {
        this.types = types;
        return this;
    }

    public List<UUID> getPlugins() {
        return plugins;
    }

    public ProcessorFilter setPlugins(List<UUID> plugins) {
        this.plugins = plugins;
        return this;
    }

    @Override
    public void build() {

        if (JdbcUtils.isValid(modules)) {
            addToWhere(JdbcUtils.in("processor.str_module", modules.size()));
            addToValues(modules);
        }

        if (JdbcUtils.isValid(types)) {
            addToWhere(JdbcUtils.in("processor.int_type", types.size()));
            for (ProcessorType type: types) {
                addToValues(type.ordinal());
            }
        }

        if (JdbcUtils.isValid(shortNames)) {
            addToWhere(JdbcUtils.in("processor.str_short_name", shortNames.size()));
            addToValues(shortNames);
        }

        if (JdbcUtils.isValid(names)) {
            addToWhere(JdbcUtils.in("processor.str_name", names.size()));
            addToValues(names);
        }

        if (JdbcUtils.isValid(plugins)) {
            addToWhere(JdbcUtils.in("plugin.pk_plugin", plugins.size()));
            addToValues(plugins);
        }
    }

    @Override
    public Map<String, String> getSortMap() {
        return sortMap;
    }
}
