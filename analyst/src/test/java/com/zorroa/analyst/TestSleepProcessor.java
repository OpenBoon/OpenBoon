package com.zorroa.analyst;

import com.zorroa.sdk.processor.DocumentProcessor;
import com.zorroa.sdk.processor.Frame;

/**
 * Created by chambers on 10/21/16.
 */
public class TestSleepProcessor extends DocumentProcessor {

    @Override
    public void process(Frame frame) throws Exception {
        Thread.sleep(2000);
    }
}
