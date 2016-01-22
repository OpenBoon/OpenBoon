package com.zorroa.archivist.ingestors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.domain.ProxyOutput;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import com.zorroa.archivist.sdk.service.EventLogService;
import com.zorroa.archivist.sdk.service.ImageService;
import com.zorroa.archivist.sdk.util.Json;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Flip;
import net.coobird.thumbnailator.filters.ImageFilter;
import net.coobird.thumbnailator.filters.Rotation;
import net.coobird.thumbnailator.resizers.configurations.Rendering;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ProxyProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyProcessor.class);

    @Autowired
    ImageService imageService;

    @Autowired
    EventLogService eventLogService;

    public ProxyProcessor() { }

    @Override
    public void process(AssetBuilder asset) {
        if (asset.contains("proxies")) {
            logger.debug("Proxy images already exist for {}", asset);
            //TODO: check if config changed.
            return;
        }

        if (asset.getImage() == null)  {
            logger.debug("There is no image metadata for making a proxy.");
            return;
        }

        List<ProxyOutput> outputs = Json.Mapper.convertValue(getArgs().get("proxies"),
                new TypeReference<List<ProxyOutput>>() {});

        if (outputs == null) {
            String format = imageService.getDefaultProxyFormat();
            outputs = Lists.newArrayList(
                    new ProxyOutput(format, 128, 8, 0.5f),
                    new ProxyOutput(format, 256, 8, 0.7f),
                    new ProxyOutput(format, 1024, 8, 0.9f)
            );
        }

        int width = asset.getImage().getWidth();
        ProxySchema result = new ProxySchema();

        for (ProxyOutput output : outputs) {
            if (output.getSize() < width) {
                addResult(asset, output, result);
            } else {
                if (result.size() == 0) {
                    // No proxies generated, copy the source file as a proxy
                    // but use a lower quality and the standard proxy format
                    String format = imageService.getDefaultProxyFormat();
                    ProxyOutput sourceProxy = new ProxyOutput(format, width, 8, 0.5f);
                    addResult(asset, sourceProxy, result);
                }
                break;
            }
        }

        if (!result.isEmpty()) {
            Collections.sort(result, (o1, o2) ->
                    Ints.compare(o1.getWidth() * o1.getHeight(), o2.getWidth() * o2.getHeight()));

            asset.getDocument().put("tinyProxy", makeTinyProxy(result.get(0)));
            asset.addSchema(result);
        }
    }

    private void addResult(AssetBuilder asset, ProxyOutput output, ProxySchema result) {
        try {
            result.add(makeProxy(asset.getImage(), output, getOrientationFilters(asset)));
        } catch (IOException e) {
            /*
             * If we fail to make a proxy, then throw, its probably a bad file.  We could also
             * fail if we don't have any proxies at all
             */
            throw new UnrecoverableIngestProcessorException("Failed to make proxy of:" + asset.getAbsolutePath(),
                    e, getClass());
        }
    }

    private static final List<String> NO_TINY_PROXY = ImmutableList.of(
            "#FF0000", "#FFFFFF", "#FF0000",
            "#FFFFFF", "#FF0000", "#FFFFFF",
            "#FF0000", "#FFFFFF", "#FF0000");

    private List<String> makeTinyProxy(Proxy smallest) {
        try {
            // Create a 3x3 proxy, avoid borders and blurring by downsampling
            // to an 11x11 image, ignoring the outer frame, and taking the
            // center pixel of each 3x3 block.
            File proxyFile = new File(smallest.getPath());
            BufferedImage source = ImageIO.read(proxyFile);
            BufferedImage tinyImage = new BufferedImage(11, 11, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tinyImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(source, 0, 0, 11, 11, null);
            g2.dispose();

            List<String> colors = Lists.newArrayListWithCapacity(9);
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    Color c = new Color(tinyImage.getRGB(x * 3 + 2, y * 3 + 2));
                    colors.add(String.format("#%02x%02x%02x", c.getRed(),c.getGreen(),c.getBlue()));
                }
            }

            return colors;

        } catch (IOException e) {
            logger.warn("Failed to create tiny proxy of " + smallest.getPath() + "," + e, e);
        }

        return NO_TINY_PROXY;
    }

    public Proxy makeProxy(BufferedImage image, ProxyOutput output) throws IOException {
        return makeProxy(image, output, Lists.newArrayListWithCapacity(0));
    }

    public Proxy makeProxy(BufferedImage image, ProxyOutput output, List<ImageFilter> filters) throws IOException {
        String proxyId = UUID.randomUUID().toString();
        File outFile = imageService.allocateProxyPath(proxyId, output.getFormat());

            Thumbnails.of(image)
                .width(output.getSize())
                .outputFormat(output.getFormat())
                .addFilters(filters)
                .keepAspectRatio(true)
                .imageType(BufferedImage.TYPE_INT_RGB)
                .rendering(Rendering.QUALITY)
                .outputQuality(output.getQuality())
                .toFile(outFile);

        Proxy result = new Proxy();
        result.setPath(outFile.getAbsolutePath());
        result.setWidth(output.getSize());
        result.setHeight(Math.round(output.getSize() / (image.getWidth() / (float)image.getHeight())));
        result.setFormat(output.getFormat());
        return result;
    }

    /**
     * EXIF orientation matrix.
     */
    private static final int NONE = 0;
    private static final int HORIZONTAL = 1;
    private static final int VERTICAL = 2;
    private static final int[][] ORIENT_MATRIX = new int[][] {
            new int[] {  0, NONE},
            new int[] {  0, HORIZONTAL},
            new int[] {180, NONE},
            new int[] {180, HORIZONTAL},
            new int[] { 90, HORIZONTAL},
            new int[] { 90, NONE},
            new int[] {270, HORIZONTAL},
            new int[] {270, NONE},
    };

    /**
     * Read in the Exif.Orientation flag and return a list of
     * image filters that will properly rotate/flip the image.
     *
     * @param asset
     * @return
     */
    private static List<ImageFilter> getOrientationFilters(AssetBuilder asset) {

        List<ImageFilter> filters = Lists.newArrayListWithCapacity(2);

        try {
            Integer index = asset.getAttr("Exif.Orientation");  // Throws NPE on failure
            index -= 1;         // Exif.Orientation is [1,8]
            if (index < 0) {    // Clamp, just in case
                index = 0;
            } else if (index > 7) {
                index = 7;
            }

            int degrees = ORIENT_MATRIX[index][0];
            if (degrees!= 0) {
                /*
                 * System quietly ignores negative degrees, so we have to
                 * subtract from 360
                 */
                filters.add(Rotation.newRotator(degrees));
            }
            switch(ORIENT_MATRIX[index][1])  {
                case HORIZONTAL:
                    filters.add(Flip.HORIZONTAL);
                    break;
                case VERTICAL:
                    filters.add(Flip.VERTICAL);
                    break;
            }
        } catch (Exception e) {
            logger.warn("Failed to determine image orientation: {}", asset, e);
        }
        return filters;
    }
}
