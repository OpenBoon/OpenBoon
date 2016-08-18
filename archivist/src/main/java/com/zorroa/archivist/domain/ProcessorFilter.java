package com.zorroa.archivist.domain;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.repository.DaoFilter;

import java.util.List;

/**
 * Created by chambers on 8/17/16.
 */
public class ProcessorFilter extends DaoFilter {

    private List<String> modules;
    private List<String> names;
    private List<String> shortNames;
    private List<String> types;
    private List<Integer> plugins;

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

    public List<String> getTypes() {
        return types;
    }

    public ProcessorFilter setTypes(List<String> types) {
        this.types = types;
        return this;
    }

    public List<Integer> getPlugins() {
        return plugins;
    }

    public ProcessorFilter setPlugins(List<Integer> plugins) {
        this.plugins = plugins;
        return this;
    }

    @Override
    public void build() {

        if (JdbcUtils.isValid(modules)) {
            where.add(JdbcUtils.in("processor.str_module", modules.size()));
            values.addAll(modules);
        }

        if (JdbcUtils.isValid(types)) {
            where.add(JdbcUtils.in("processor.str_type", types.size()));
            values.addAll(types);
        }

        if (JdbcUtils.isValid(shortNames)) {
            where.add(JdbcUtils.in("processor.str_short_name", shortNames.size()));
            values.addAll(shortNames);
        }

        if (JdbcUtils.isValid(names)) {
            where.add(JdbcUtils.in("processor.str_name", names.size()));
            values.addAll(names);
        }

        if (JdbcUtils.isValid(plugins)) {
            where.add(JdbcUtils.in("plugin.pk_plugin", plugins.size()));
            values.addAll(plugins);
        }
    }
}
