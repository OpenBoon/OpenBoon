package com.zorroa.analyst.ingestors;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by chambers on 4/26/16.
 */
public class DatabaseIngestorTests extends AbstractTest {

    @Test
    public void process() {

        /*
         * Setups up and in memory DB.
         */
        DatabaseIngestor db = new DatabaseIngestor();
        db.namespace = "database";
        db.driverClass="org.h2.Driver";
        db.connectionUri="jdbc:h2:mem:test";
        db.username="sa";
        db.query="SELECT * FROM test WHERE str_path=${source.path}";
        db.init();

        /*
         * Use the JDBC connection that is part of the ingestor to setup
         * some test data.
         */
        db.getJdbc().update("CREATE TABLE test (str_path VARCHAR(1024), int_value INTEGER, float_value FLOAT)");

        for (File file: new File("src/test/resources/images").listFiles()) {
            db.getJdbc().update("INSERT INTO test (str_path, int_value, float_value) VALUES (?,?,?)",
                    file.getAbsolutePath(), 100, 1.0);
        }

        AssetBuilder asset = new AssetBuilder(
                new File("src/test/resources/images/toucan.jpg").getAbsoluteFile());
        db.process(asset);

        assertTrue(asset.attrExists("database"));
        assertEquals(asset.getAbsolutePath(), asset.getAttr("database.STR_PATH"));
        assertEquals(100, (int) asset.getAttr("database.INT_VALUE"));
        assertEquals(1.0, asset.getAttr("database.FLOAT_VALUE"));
    }
}
