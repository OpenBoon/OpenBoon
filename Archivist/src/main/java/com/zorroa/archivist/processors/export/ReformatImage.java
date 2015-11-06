package com.zorroa.archivist.processors.export;

import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.processor.export.Port;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Rendering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Created by chambers on 11/1/15.
 */
public class ReformatImage extends ExportProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ReformatImage.class);

    public final Port<String> inputPath;
    public final Port<Dimension> size;
    public final Port<String> format;
    public final Port<String> basename;

    public final Port<String> outputPath;

    public ReformatImage() {
        inputPath = new Port<>("inputPath", Port.Type.Input, this);
        size = new Port<>("size",  Port.Type.Input, this);
        format = new Port<>("format", "%{source.extension}",  Port.Type.Input, this);
        basename = new Port<>("basename", "%{source.basename}", Port.Type.Input, this);

        outputPath = new Port<>("outputPath", Port.Type.Output, this);
    }

    public void process() throws Exception {
        /*
         *  There might be multiple cords plugin in here, so iterate
         *  over all the values.
         */
        for (String value: inputPath.getValues()) {

            Thumbnails.Builder builder = Thumbnails.of(value)
                    .rendering(Rendering.QUALITY);

            if (size.getValue() != null) {
                Dimension newSize = size.getValue();
                builder.size(newSize.width, newSize.height);
                builder.keepAspectRatio(true);
            }

            if (format.getValue() != null) {
                builder.outputFormat(format.getValue());
            }

            /*
             * Create an output path
             */
            String output = String.format("%s/%s.%s", getWorkingDirectory(), basename.getValue(), format.getValue());
            builder.toFile(output);
            outputPath.addValue(output);
        }
    }
}
