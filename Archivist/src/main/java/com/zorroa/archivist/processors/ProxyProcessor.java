package com.zorroa.archivist.processors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.domain.Proxy;
import com.zorroa.archivist.domain.ProxyOutput;
import com.zorroa.archivist.service.ImageService;

public class ProxyProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyProcessor.class);

    @Autowired
    protected ImageService imageService;

    public ProxyProcessor() { }

    @Override
    public void process(AssetBuilder asset) {

        List<Proxy> result = Lists.newArrayList();
        for (ProxyOutput output: getProxyConfig().getOutputs()) {
            try {
                result.add(imageService.makeProxy(asset.getFile(), output));
            } catch (IOException e) {
                logger.warn("Failed to create proxy {}, ", output, e);
            }
        }

        Collections.sort(result, new Comparator<Proxy>() {
            @Override
            public int compare(Proxy o1, Proxy o2) {
                return Ints.compare(o1.getWidth() * o1.getHeight(), o2.getWidth() * o2.getHeight());
            }
        });

        if (!result.isEmpty()) {
            asset.document.put("tinyProxy", makeTinyProxy(result.get(0)));
            asset.document.put("proxies", result);
        }
    }

    public static final List<String> NO_TINY_PROXY = ImmutableList.of(
            "#FF0000", "#FFFFFF", "#FF0000",
            "#FFFFFF", "#FF0000", "#FFFFFF",
            "#FF0000", "#FFFFFF", "#FF0000");

    public List<String> makeTinyProxy(Proxy smallest) {
        try {
            BufferedImage source = ImageIO.read(imageService.generateProxyPath(smallest.getFile()));
            BufferedImage tinyImage = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tinyImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(source, 0, 0, 3, 3, null);
            g2.dispose();

            List<String> colors = Lists.newArrayListWithCapacity(9);
            for (int y = 0; y < tinyImage.getHeight(); y++) {
                for (int x = 0; x < tinyImage.getWidth(); x++) {
                    Color c = new Color(tinyImage.getRGB(x, y));
                    colors.add(String.format("#%02x%02x%02x", c.getRed(),c.getGreen(),c.getBlue()));
                }
            }

            return colors;

        } catch (IOException e) {
            logger.warn("Failed to create tiny proxy of " + smallest.getFile() + "," + e, e);
        }

        return NO_TINY_PROXY;
    }
}
