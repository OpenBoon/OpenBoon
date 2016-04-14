package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Attr;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
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

import static com.zorroa.archivist.sdk.domain.Attr.attr;
import static org.bytedeco.javacpp.lept.pixDestroy;
import static org.bytedeco.javacpp.lept.pixRead;

/**
 * Created by jbuhler on 2/17/16.
 */
public class OCRIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OCRIngestor.class);

    public static class Box {
        public List<Integer> boxArea;
        public String namespace;
        public String key;
        public boolean isKeyword;

        public Box() {}

        public Box(List<Integer> boxArea, String namespace, String key, boolean isKeyword) {
            this.boxArea = boxArea;
            this.namespace = namespace;
            this.key = key;
            this.isKeyword = isKeyword;
        }
    }

    public static class Option {
        public List<String> targetString;
        public List<Integer> targetStringBox;
        public String namespace;
        public String key;
        public String value;
        public List<Box> boxes;

        public Option() {}

        public Option(List<String> targetString, List<Integer> targetStringBox, String namespace, String key, String value, List<Box> boxes) {
            this.targetString = targetString;
            this.targetStringBox = targetStringBox;
            this.namespace = namespace;
            this.key = key;
            this.value = value;
            this.boxes = boxes;
        }
    }


    @Argument
    public List<Option> docTypes;

    Set<String> words;

    tesseract.TessBaseAPI api;

    public void init() {

        super.init();
        ImmutableSet.Builder builder = ImmutableSet.builder();
        String wordsPath = applicationProperties.getString("analyst.path.models") + "/ocr/words";


        try {
            /*
             * This is the dictionary for OSX but it may be someplace else on linux.
             */
            Files.readLines(new File(wordsPath), Charset.defaultCharset(), new LineProcessor<String>() {
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

        String tessPath = applicationProperties.getString("analyst.path.models") + "/ocr/tessdata";

        api = new tesseract.TessBaseAPI();
        if (api.Init(tessPath, "eng") != 0) {
            logger.error("Could not initialize tesseract.");
            return;
        }

    }

    @Override
    public void process(AssetBuilder asset) {

        if (!asset.isSuperType("image")) {
            return;
        }

        logger.info("Processing {}", asset.getAbsolutePath());
        lept.PIX image = pixRead(asset.getAbsolutePath());
        if (image.h() > 8089 || image.w() > 8096) {
            logger.warn("large image: {}x{}, {}", image.w(), image.h(), asset.getAbsolutePath());
        }

        api.SetImage(image);

        try {

            for ( Option docType: docTypes) {
                if (handle_doc(api, image, asset, docType)) {
                    return;
                }
            }
        }
        finally {
            pixDestroy(image);
        }
    }

    @Override
    public void teardown() {
        api.End();
    }

    public boolean handle_doc(tesseract.TessBaseAPI api, lept.PIX image, AssetBuilder asset, Option docType) {

        List<Integer> box = docType.targetStringBox;

        if (!detect(api, image, box.get(0), box.get(1), box.get(2), box.get(3), docType.targetString)) {
            return false;
        }

        asset.setAttr(attr(docType.namespace, docType.key), docType.value);
        asset.addSuggestKeywords("ocr", docType.value);

        for (Box ocrBox: docType.boxes) {
            String text = getText(api, image, ocrBox.boxArea.get(0), ocrBox.boxArea.get(1), ocrBox.boxArea.get(2), ocrBox.boxArea.get(3));
            asset.setAttr(attr(ocrBox.namespace, ocrBox.key), text);
            asset.setAttr(attr(ocrBox.namespace, ocrBox.key), text);
            if (ocrBox.isKeyword) {
                asset.addKeywords("ocr", text);
            }

        }

        return true;
    }

    public boolean detect(tesseract.TessBaseAPI api, lept.PIX image, int x, int y, int width, int height, List<String> text) {


        if (x < 0) {
            x = image.w() - x;
        }
        if (y < 0) {
            y = image.h() - y;
        }
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
