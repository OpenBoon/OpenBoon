package com.zorroa.common.elastic;

import org.elasticsearch.Version;
import org.elasticsearch.common.cli.Terminal;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;

import java.util.Collection;

/**
 * Created by chambers on 4/4/16.
 */
public class ZorroaNode extends Node {

    public ZorroaNode(Settings settings, Collection<Class<? extends Plugin>> classpathPlugins) {
        super(InternalSettingsPreparer.prepareEnvironment(settings, (Terminal)null), Version.CURRENT, classpathPlugins);
    }
}
