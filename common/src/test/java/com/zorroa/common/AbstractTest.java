package com.zorroa.common;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.config.CommonBeanConfig;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.util.AssetUtils;
import com.zorroa.sdk.util.FileUtils;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by chambers on 2/17/16.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("/test.properties")
@WebAppConfiguration
@ContextConfiguration(loader=AnnotationConfigContextLoader.class, classes={UnitTestConfiguration.class, CommonBeanConfig.class})
public abstract class AbstractTest {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ApplicationProperties applicationProperties;

    @Autowired
    protected Client client;

    public AbstractTest() {
        System.setProperty("zorroa.unittest", "true");
    }

    public Path getTestImagePath(String subdir) {
        return FileUtils.normalize(Paths.get("../unittest/resources/images").resolve(subdir));
    }

    public Path getTestImagePath() {
        return getTestImagePath("set04/standard");
    }

    private static final Set<String> SUPPORTED_FORMATS = ImmutableSet.of(
            "jpg", "pdf", "mov", "gif", "tif");

    public List<Source> getTestAssets(String subdir) {
        List<Source> result = Lists.newArrayList();
        for (File f: getTestImagePath(subdir).toFile().listFiles()) {

            if (f.isFile()) {
                if (SUPPORTED_FORMATS.contains(FileUtils.extension(f.getPath()).toLowerCase())) {
                    Source b = new Source(f);
                    b.setAttr("user.rating", 4);
                    b.setAttr("test.path", getTestImagePath(subdir).toAbsolutePath().toString());
                    AssetUtils.addKeywords(b, "source", b.getAttr("source.filename", String.class));
                    result.add(b);
                }
            }
        }

        for (File f: getTestImagePath(subdir).toFile().listFiles()) {
            if (f.isDirectory()) {
                result.addAll(getTestAssets(subdir + "/" + f.getName()));
            }
        }

        return result;
    }

    @Before
    public void __init() throws IOException, ClassNotFoundException {
        /**
         * For analyst, this is only done for unit tests.  In production, the
         * archivist handles creating the index and the mapping.
         */
        ElasticClientUtils.deleteAllIndexes(client);
        ElasticClientUtils.createLatestMapping(client, "archivist");
        ElasticClientUtils.createLatestMapping(client, "analyst");
        ElasticClientUtils.createIndexedScripts(client);
        ElasticClientUtils.createEventLogTemplate(client);
    }

    public void refreshIndex() {
        ElasticClientUtils.refreshIndex(client);
    }

    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists()) {
            return true;
        }
        boolean ret = true;
        if (path.isDirectory()){
            for (File f : path.listFiles()){
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

    public String getMappingType(Map<String,Object> mapping, String path) {
        for (String e: path.split("\\.")) {
            mapping = (Map<String,Object>) mapping.get("properties");
            mapping = (Map<String,Object>) mapping.get(e);
        }
        logger.info("{}", mapping.get("type"));
        return mapping.get("type").toString();
    }
}
