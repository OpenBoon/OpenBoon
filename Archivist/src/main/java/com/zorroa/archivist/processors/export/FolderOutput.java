package com.zorroa.archivist.processors.export;

import com.google.common.io.Files;
import com.zorroa.archivist.FileUtils;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.processor.export.Port;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

/**
 * Created by chambers on 11/6/15.
 */
public class FolderOutput extends ExportProcessor {

    @Value("${archivist.export.outputPath}")
    String defaultOutputPath;

    public final Port<String> inputPath;
    public final Port<String> outputPath;

    public FolderOutput() {
        inputPath = new Port<>("inputPath", Port.Type.Input, this);
        outputPath = new Port<>("outputPath", Port.Type.Output, this);
    }

    @Override
    public void init() throws Exception {
        /*
        * Injected values are not available at construction time.
        */
        outputPath.setDefault(defaultOutputPath);
    }

    @Override
    protected void process() throws Exception {
        String _outputPath = outputPath.getValue();
        FileUtils.makedirs(_outputPath);

        for (String input: inputPath.getValues()) {
            Files.copy(new File(input), new File(FileUtils.pathjoin(_outputPath, FileUtils.filename(input))));
        }
    }
}
