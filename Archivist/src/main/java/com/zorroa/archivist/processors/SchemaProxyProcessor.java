package com.zorroa.archivist.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.AssetType;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.domain.ProxyOutput;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ImageSchema;
import com.zorroa.archivist.sdk.service.ImageService;
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

public class SchemaProxyProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SchemaProxyProcessor.class);

    @Autowired
    protected ImageService imageService;

    public SchemaProxyProcessor() { }

    protected  List<ProxyOutput> parseProxyOutput(String key) {
        try {   // Re-parse from generic args Map to List<ProxyOutput>
            ObjectMapper mapper = new ObjectMapper();
            Object proxyList = getArgs().get("proxies");
            return mapper.readValue(mapper.writeValueAsString(proxyList), new TypeReference<List<ProxyOutput>>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void process(AssetBuilder asset) {
        List<ProxyOutput> outputs = parseProxyOutput("proxies");
        if (outputs == null) {
            String format = imageService.getDefaultProxyFormat();
            outputs = Lists.newArrayList(
                    new ProxyOutput(format, 128, 8, 0.5f),
                    new ProxyOutput(format, 256, 8, 0.7f),
                    new ProxyOutput(format, 1024, 8, 0.9f)
            );
        }
        if (asset.isType(AssetType.Image)) {
            extractDimensions(asset);
            int width = asset.getAttr("image", "width");
            List<Proxy> result = Lists.newArrayList();
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
                asset.getDocument().put("proxies", result);
            }
        }
    }

    private void addResult(AssetBuilder asset, ProxyOutput output, List<Proxy> result) {
        try {
            result.add(imageService.makeProxy(asset.getFile(), output));
        } catch (IOException e) {
            logger.warn("Failed to create proxy {}: " + e.getMessage(), output);
            asset.put("errors", "SchemaProxyProcessor", e.getMessage());
        }
    }

    private void extractDimensions(AssetBuilder asset) {
        try {
            Dimension size = imageService.getImageDimensions(asset.getFile());
            ImageSchema schema = asset.getSchema("image", ImageSchema.class);
            schema.setWidth(size.width);
            schema.setHeight(size.height);
        } catch (IOException e) {
            logger.warn("Unable to determine image dimensions: {}: " + e.getMessage(), asset);
            asset.put("errors", getClass().getCanonicalName(), e.getMessage());
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
}
