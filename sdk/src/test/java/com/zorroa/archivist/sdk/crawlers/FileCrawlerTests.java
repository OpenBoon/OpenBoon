package com.zorroa.archivist.sdk.crawlers;

import com.zorroa.archivist.sdk.domain.AnalyzeRequestEntry;
import com.zorroa.archivist.sdk.util.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 4/6/16.
 */
public class FileCrawlerTests {

    final static Logger logger = LoggerFactory.getLogger(FileCrawlerTests.class);

    @Test
    public void testSequences() throws IOException {

        Consumer<AnalyzeRequestEntry> consumer = entry -> {
            assertEquals("0001-0004", entry.getAttrs().get("sequence.range"));
            assertEquals(4, entry.getAttrs().get("sequence.zfill"));
            assertEquals("foo_bar.#.txt", FileUtils.filename((String) entry.getAttrs().get("sequence.spec")));
            assertEquals("#", entry.getAttrs().get("sequence.padding"));
            assertEquals("foo_bar.0001.txt",  FileUtils.filename(entry.getUri()));
        };

        URI uri = new File("src/test/resources/sequences").toURI();

        FileCrawler c = new FileCrawler();
        c.start(uri, consumer);
    }
}
