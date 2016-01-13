package com.zorroa.ingestors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.DocumentSchema;
import com.zorroa.archivist.sdk.util.StringUtil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import static org.bytedeco.javacpp.lept.pixDestroy;
import static org.bytedeco.javacpp.lept.pixRead;

/**
 * Created by chambers on 1/12/16.
 */
public class OilWellLogIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OilWellLogIngestor.class);

    Set<String> words;

    public void init() {

        ImmutableSet.Builder builder = ImmutableSet.builder();

        try {
            /*
             * This is the dictionary for OSX but it may be someplace else on linux.
             */
            Files.readLines(new File("/usr/share/dict/words"), Charset.defaultCharset(), new LineProcessor<String>() {
                @Override
                public boolean processLine(String s) throws IOException {
                    builder.add(s);
                    return true;
                }

                @Override
                public String getResult() {
                    return null;
                }
            });
        } catch (IOException e) {
            logger.warn("unable to open dictionary:", e);
        }

        words = builder.build();
    }

    @Override
    public void process(AssetBuilder assetBuilder) {

        tesseract.TessBaseAPI api = new tesseract.TessBaseAPI();
        if (api.Init("/usr/local/share/tessdata", "eng") != 0) {
            logger.error("Could not initialize tesseract.");
            return;
        }

        logger.info("Processing {}", assetBuilder.getAbsolutePath());
        lept.PIX image = pixRead(assetBuilder.getAbsolutePath());
        if (image.h() > 8089 || image.w() > 8096) {
            logger.warn("large image: {}x{}, {}", image.w(), image.h(), assetBuilder.getAbsolutePath());
        }

        api.SetImage(image);

        try {

            if (handle_BHP_SWC_PHOTOGRAPHY(api, image, assetBuilder)) {
                return;
            }

            if (handle_BHP_CORE_SAMPLE_HORZ(api, image, assetBuilder)) {
                return;
            }

            if (handle_BHP_CORE_SAMPLE_MULTI(api, image, assetBuilder)) {
                return;
            }

            if (handle_INPEX_SWC_SAMPLES(api, image, assetBuilder)) {
                return;
            }

            if (handle_OZALID(api, image, assetBuilder)) {
                return;
            }

            if (handle_WELL_LOG_LONG(api, image, assetBuilder)) {
                return;
            }

            if (handle_ESSO_AU_MAP(api, image, assetBuilder)) {
                return;
            }

            handleDefault(api, image, assetBuilder);
        }
        finally {
            api.End();
            pixDestroy(image);
        }
    }

    public boolean handle_BHP_SWC_PHOTOGRAPHY(tesseract.TessBaseAPI api, lept.PIX image, AssetBuilder assetBuilder) {
        if (!detect(api, image, 766, 0, 1617, 542, "SWC", "PHOTOGRAPHY")) {
            return false;
        }

        logger.info("Handling BHP_SWC_PHOTOGRAPHY: {}", assetBuilder.getAbsolutePath());

        DocumentSchema doc = new DocumentSchema();
        doc.setTitle(getText(api, image, 766, 0, 1617, 542));
        doc.setBody(getText(api, image, 0, 1975, 3012, 258));

        assetBuilder.setAttr("wellLog", "type", "BHP_SWC_PHOTOGRAPHY");
        assetBuilder.addKeywords(1, false, doc.getBody());
        assetBuilder.addSchema(doc);
        return true;
    }

    public boolean handle_BHP_CORE_SAMPLE_HORZ(tesseract.TessBaseAPI api, lept.PIX image, AssetBuilder assetBuilder) {
        if (!detect(api, image, 865, 143, 657, 150, "THEBE")) {
            return false;
        }

        logger.info("Handling BHP_CORE_SAMPLE_HORZ: {}", assetBuilder.getAbsolutePath());
        assetBuilder.setAttr("wellLog", "type", "BHP_CORE_SAMPLE_HORZ");

        DocumentSchema doc = new DocumentSchema();
        doc.setTitle(getText(api, image, 540, 0, 1556, 319));

        String meters = getText(api, image, 53, 358, 550, 205);
        String corenum =  getText(api, image, 1791, 358, 535, 169);

        try {
            assetBuilder.setAttr("coreSample", "meters", Double.valueOf(meters.replace("m", "")));
            assetBuilder.setAttr("coreSample", "number", StringUtil.findInteger(corenum));
        } catch (Exception e) {
            logger.warn("Failed to set coreSample properties, ", e);
        }

        assetBuilder.addKeywords(1, false, "BHP", "Billiton", "core", "sample");
        assetBuilder.addSchema(doc);
        return true;
    }

    public boolean handle_BHP_CORE_SAMPLE_MULTI(tesseract.TessBaseAPI api, lept.PIX image, AssetBuilder assetBuilder) {
        if (!detect(api, image, 0, 0, image.w(), 276, "CORE NO", "BHP")) {
            return false;
        }

        logger.info("Handling BHP_CORE_SAMPLE_MULTI: {}", assetBuilder.getAbsolutePath());
        assetBuilder.setAttr("wellLog", "type", "BHP_CORE_SAMPLE_MULTI");

        DocumentSchema doc = new DocumentSchema();
        doc.setTitle(getText(api, image, 668, 0, 1366, 295));
        assetBuilder.addKeywords(1, false, "BHP", "Billiton", "core", "sample");
        assetBuilder.addSchema(doc);
        return true;
    }

    public boolean handle_OZALID(tesseract.TessBaseAPI api, lept.PIX image, AssetBuilder assetBuilder) {

        if (!detect(api, image, 0, 0, image.w(), 1800, "OZALID")) {
            return false;
        }
        logger.info("Handling OZALID: {}", assetBuilder.getAbsolutePath());
        assetBuilder.setAttr("wellLog", "type", "OZALID");

        String text = getText(api, image, 0, 0, image.w(), 1800);
        if (text != null) {
            try {
                DocumentSchema doc = new DocumentSchema();
                doc.setTitle(text.substring(text.indexOf("R0")).replaceAll("\\W", ""));
                assetBuilder.addSchema(doc);
            } catch (Exception ignore) {

            }
        }

        return true;
    }

    public boolean handle_WELL_LOG_LONG(tesseract.TessBaseAPI api, lept.PIX image, AssetBuilder assetBuilder) {

        if (!detect(api, image, 0, 0, image.w(), 1174, "TYPE OF")) {
            return false;
        }
        logger.info("Handling WELL_LOG_LONG: {}", assetBuilder.getAbsolutePath());
        assetBuilder.setAttr("wellLog", "type", "WELL_LOG_LONG");
        assetBuilder.addKeywords(1, false, getValidatedWordList(api, image, 0, 0, image.w(), 1174));

        return true;
    }

    public boolean handle_INPEX_SWC_SAMPLES(tesseract.TessBaseAPI api, lept.PIX image, AssetBuilder assetBuilder) {

        if (!detect(api, image, 320, 0, 661, 325, "INPEX", "SWC", "SAMPLES")) {
            return false;
        }
        logger.info("Handling INPEX_SWC_SAMPLES: {}", assetBuilder.getAbsolutePath());
        assetBuilder.setAttr("wellLog", "type", "INPEX_SWC_SAMPLES");
        /*
         * Seems to have problems with the font, its not monospaced
         */
        assetBuilder.addKeywords(1, true, "inpex", "browse", "limited", "swc", "samples");
        return true;
    }

    public boolean handle_ESSO_AU_MAP(tesseract.TessBaseAPI api, lept.PIX image, AssetBuilder assetBuilder) {

        if (!detect(api, image, image.w()-1480, image.h()-1480, 1475, 1475, "ESSO")) {
            return false;
        }
        logger.info("Handling ESSO_AU_MAP: {}", assetBuilder.getAbsolutePath());
        assetBuilder.setAttr("wellLog", "type", "ESSO_AU_MAP");

        /*
         * Seems to have problems with the font, its not monospaced
         */
        assetBuilder.addKeywords(1, true, "esso", "map");
        assetBuilder.addKeywords(1, true, getValidatedWordList(api, image, image.w()-1480, image.h()-1480, 1475, 1475));

        return true;
    }

    public boolean handleDefault(tesseract.TessBaseAPI api, lept.PIX image, AssetBuilder assetBuilder) {

        logger.info("Handling DEFAULT {}", assetBuilder.getAbsolutePath());
        assetBuilder.setAttr("wellLog", "type", "DEFAULT");

        List<String> detectedWords = getValidatedWordList(api, image, 0, 0,
                Math.min(4096,image.w()), Math.min(4096, image.h()));
        if (!detectedWords.isEmpty()) {
            DocumentSchema schema = new DocumentSchema();
            schema.setBody(String.join(" ", detectedWords));
            assetBuilder.addKeywords(1, false, detectedWords);
            assetBuilder.addSchema(schema);
            return true;
        }
        return false;
    }

    public boolean detect(tesseract.TessBaseAPI api, lept.PIX image, int x, int y, int width, int height, String ... text) {

        /*
         *  The image is too small
         */
        if (x+width > image.w() || y+height > image.h()) {
            return false;
        }

        api.SetRectangle(x, y, width, height);
        BytePointer bp = api.GetUTF8Text();

        if (bp == null) {
            return false;
        }

        try {
            String str = bp.getString().toUpperCase();
            for (String t: text) {
                if (!str.contains(t)) {
                    return false;
                }
            }
            return true;
        }
        finally {
            bp.deallocate();
        }
    }

    public String getText(tesseract.TessBaseAPI api, lept.PIX image, int x, int y, int width, int height) {

        if (x+width > image.w() || y+height > image.h()) {
            return null;
        }

        api.SetRectangle(x, y, width, height);
        BytePointer bp = api.GetUTF8Text();
        if (bp == null) {
            return null;
        }

        try {
            String result = bp.getString();
            return result.replaceAll("\\R|\\p{javaSpaceChar}", " ");
        }
        finally {
            bp.deallocate();
        }
    }

    public Iterable<String> getWordStream(tesseract.TessBaseAPI api, lept.PIX image, int x, int y, int width, int height) {
        return StringUtil.getWordStream(getText(api, image, x, y, width, height));
    }

    public List<String> getWordList(tesseract.TessBaseAPI api, lept.PIX image, int x, int y, int width, int height) {
        return StringUtil.getWordList(getText(api, image, x, y, width, height));
    }

    public List<String> getValidatedWordList(tesseract.TessBaseAPI api, lept.PIX image, int x, int y, int width, int height) {
        List<String> result = Lists.newArrayListWithCapacity(100);
        for (String word: StringUtil.getWordStream(getText(api, image, x, y, width, height))) {

            word = word.toLowerCase();
            word = word.replaceAll("[^A-Za-z0-9]", "");
            if (word.length() <= 2) {
                continue;
            }

            if (words.contains(word) || words.contains(StringUtil.capitalize(word))) {
                result.add(word);
            }
        }
        return result;
    }
}
