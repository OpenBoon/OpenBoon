package com.zorroa.analyst.service;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.sdk.filesystem.ObjectFile;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 4/28/16.
 */
public class TransferServiceTests extends AbstractTest {

    @Autowired
    ObjectFileSystem objectFileSystem;

    @Autowired
    TransferService transferService;

    URI testImage = URI.create("http://www.zorroa.com/wp-content/uploads/2015/06/zorroa-logo-square-small-grey-e1433920050364.png");

    @Test
    public void testTransfer() throws IOException {
        ObjectFile foo = objectFileSystem.prepare("test", testImage, "png");
        assertFalse(foo.exists());
        transferService.transfer(testImage, foo);
        assertTrue(foo.exists());
    }

}
