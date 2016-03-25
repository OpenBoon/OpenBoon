package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.filesystem.ObjectFile;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import com.zorroa.archivist.sdk.schema.SourceSchema;
import com.zorroa.archivist.sdk.util.FileUtils;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Flip;
import net.coobird.thumbnailator.filters.ImageFilter;
import net.coobird.thumbnailator.resizers.configurations.Rendering;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProxyProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyProcessor.class);

    // Argument option that specifies how to output a single proxy
    static class Output {
        public int size;
        public int bpp;
        public String format;
        public float quality;

        public Output() {}  // Required for JSON mapper due to alternate ctor below
        public Output(String format, int size, int bpp, float quality) {
            this.size = size;
            this.bpp = bpp;
            this.format = format;
            this.quality = quality;
        }
    }

    @Argument(name="proxies")
    private List<Output> outputs = new ArrayList<Output>(defaultOutputs);

    private static final String defaultProxyFormat = "jpg";
    private static final List<Output> defaultOutputs = ImmutableList.<Output>builder()
            .add(new Output(defaultProxyFormat, 1024, 8, 0.9f))
            .add(new Output(defaultProxyFormat, 256, 8, 0.7f))
            .add(new Output(defaultProxyFormat, 128, 8, 0.5f))
            .build();

    public List<Output> getOutputs() {
        return outputs;
    }

    @Override
    public void process(AssetBuilder asset) {

        if (asset.attrExists("proxies") && !asset.isChanged()) {
            logger.debug("Proxy images already exist for {}", asset);
            return;
        }

        if (asset.getImage() == null)  {
            /*
             * If no image as set by the the ingestor, look for an image represenation
             * we can use to make a proxy.
             */
            if (asset.getSource().getRepresentations() != null) {
                for(SourceSchema source: asset.getSource().getRepresentations()) {
                    if (source.getType().startsWith("image/")) {
                        try {
                            asset.setImage(ImageIO.read(new File(source.getPath())));
                        } catch (IOException e) {
                            logger.warn("Failed to open file for proxy: {}", source.getPath());
                        }
                        break;
                    }
                }
            }
            if (asset.getImage() == null) {
                logger.debug("There is no image metadata for making a proxy of '{}'", asset);
                return;
            }
        }

        ProxySchema result = new ProxySchema();

        /*
         * Sort our proxy definitions large to small so we can make subsequent proxies
         * from the largest proxy.
         */
        Collections.sort(outputs, (o1, o2) -> Integer.compare(o2.size, o1.size));

        /*
         * The first proxy is the large proxy.
         */
        BufferedImage largeProxy = null;

        try {
            final int width = asset.getImage().getWidth();
            for (Output spec : outputs) {
                if (spec.size > width) {
                    continue;
                }

                Proxy proxy;
                if (largeProxy == null) {
                    proxy = writeProxy(asset, asset.getImage(),
                            spec, getOrientationFilters(asset));
                    largeProxy = proxy.getImage();
                } else {
                    proxy = writeProxy(asset, largeProxy, spec, ImmutableList.of());
                }

                result.add(proxy);
            }

            /*
             * If the source is too small for a proxy, make it a proxy!
             */
            if (result.isEmpty()) {
                Output spec = new Output(defaultProxyFormat, width, 8, 1.0f);
                Proxy proxy = writeProxy(asset, asset.getImage(), spec, getOrientationFilters(asset));
                result.add(proxy);
            }

            /*
             * Resort the outputs so the smallest proxy is first.
             */
            Collections.sort(result, (o1, o2) ->
                    Ints.compare(o1.getWidth() * o1.getHeight(), o2.getWidth() * o2.getHeight()));

            asset.setAttr("tinyProxy", makeTinyProxy(result.get(0)));
            asset.setAttr("proxies", result);

        } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException("Failed to make proxy of:" + asset.getAbsolutePath(),
                    e, getClass());
        }
    }

    /**
     * Write a proxy of the buffered image using the settings found in Output
     * to the given Allocation.  Also apply any supplied image filters.
     *
     * @param asset
     * @param output
     * @param filters
     * @return
     * @throws IOException
     */
    private Proxy writeProxy(AssetBuilder asset,  BufferedImage image, Output output, List<ImageFilter> filters) throws Exception {
        int height = Math.round(output.size / (image.getWidth() / (float)image.getHeight()));
        ObjectFile path = objectFileSystem.get("proxies", asset.getAbsolutePath(), output.format,
                String.format("%dx%d", height, image.getWidth()));

        BufferedImage proxyImage = Thumbnails.of(image)
                .width(output.size)
                .height(height)
                .imageType(BufferedImage.TYPE_3BYTE_BGR)
                .rendering(Rendering.QUALITY)
                .addFilters(filters)
                .asBufferedImage();

        ImageWriter writer = ImageIO.getImageWritersByFormatName(output.format).next();
        /*
         * Doesn't seem to work on PNG.
         */
        if (output.format.equals("jpg")) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(output.quality);
        }

        path.mkdirs();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(path.getFile())) {
            writer.setOutput(ios);
            writer.write(proxyImage);
        } finally {
            writer.dispose();
        }

        Proxy result = new Proxy();
        result.setImage(proxyImage);
        result.setUri(objectFileSystem.getUrl(path));
        result.setName(FileUtils.filename(path.getFile().getAbsolutePath()));
        result.setWidth(output.size);
        result.setHeight(height);
        result.setFormat(output.format);
        return result;
    }

    private static final List<String> NO_TINY_PROXY = ImmutableList.of(
            "#FF0000", "#FFFFFF", "#FF0000",
            "#FFFFFF", "#FF0000", "#FFFFFF",
            "#FF0000", "#FFFFFF", "#FF0000");

    /**
     * Create a 3x3 proxy, avoid borders and blurring by downsampling
     * to an 11x11 image, ignoring the outer frame, and taking the
     * center pixel of each 3x3 block.
     *
     * @param proxy
     * @return
     */
    private List<String> makeTinyProxy(Proxy proxy) {
        BufferedImage source = proxy.getImage();
        BufferedImage tinyImage = new BufferedImage(11, 11, BufferedImage.TYPE_INT_RGB);
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
            if (degrees != 0) {
                filters.add(new ExifOrientationFilter(degrees));
            }
            switch(ORIENT_MATRIX[index][1])  {
                case HORIZONTAL:
                    filters.add(Flip.HORIZONTAL);
                    break;
                case VERTICAL:
                    filters.add(Flip.VERTICAL);
                    break;
            }

        } catch (NullPointerException e) {
            // No orientation field, no need to flip dimensions
        } catch (Exception e) {
            logger.warn("Failed to determine image orientation: {}", asset, e);
        }
        return filters;
    }

    /**
     * An AffineTransform based image rotation filter.
     */
    private static class ExifOrientationFilter implements ImageFilter {

        private final double degrees;

        public ExifOrientationFilter(double degrees) {
            this.degrees = degrees;
        }

        @Override
        public BufferedImage apply(BufferedImage original) {

            if (degrees == 0) {
                return original;
            }

            double theta = Math.toRadians(degrees);
            double cos = Math.abs(Math.cos(theta));
            double sin = Math.abs(Math.sin(theta));
            double width  = original.getWidth();
            double height = original.getHeight();
            int w = (int)(width * cos + height * sin);
            int h = (int)(width * sin + height * cos);

            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = out.createGraphics();

            double x = w/2;
            double y = h/2;
            AffineTransform at = AffineTransform.getRotateInstance(theta, x, y);
            x = (w - width)/2;
            y = (h - height)/2;
            at.translate(x, y);
            g2.drawRenderedImage(original, at);
            g2.dispose();
            return out;
        }
    }
}
