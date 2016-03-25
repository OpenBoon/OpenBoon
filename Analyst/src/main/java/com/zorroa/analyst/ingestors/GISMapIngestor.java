package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zorroa.archivist.sdk.domain.Attr.attr;

/**
 * The Map ingestor handles data in the "Maps" directory.  There is no OCR on the image because I wasn't able
 * to get any useful data.
 */
public class GISMapIngestor extends IngestProcessor {

    @Argument
    String namespace = "petrol";

    public GISMapIngestor() {
        supportedFormats.add("tif");
    }

    @Override
    public void process(AssetBuilder assetBuilder) {
        extractXmlFields(assetBuilder);
        extractTfwFields(assetBuilder);
    }

    /**
     * Reference
     * https://en.wikipedia.org/wiki/World_file
     */
    private static final String[] TFW_FIELDS = new String[] {
            "xScale", "ySkew", "xSkew", "yScale", "xPosition", "yPosition"
    };

    public void extractTfwFields(AssetBuilder assetBuilder) {
        String tfwFile = assetBuilder.getDirectory() + "/" + assetBuilder.getBasename() + ".tfw";
        try {
            List<String> lines = Files.readLines(new File(tfwFile), Charset.forName("UTF-8"));
            for (int i=0; i<TFW_FIELDS.length; i++) {
                assetBuilder.setAttr(attr(namespace, TFW_FIELDS[i]), Double.valueOf(lines.get(i).trim()));
            }

            if (lines.size() > 5 && lines.get(4) != null && lines.get(5) != null) {
                Point2D.Double location = new Point2D.Double(Double.valueOf(lines.get(4).trim()), Double.valueOf(lines.get(5).trim()));
                assetBuilder.setAttr(attr(namespace, "location"), location);
            }
        } catch (IOException e) {
            logger.warn("Failed to open TFW file: {}", tfwFile, e);
        }
    }

    /**
     * This is for parsing the key/value pairs in the body of the Process tag.
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("([A-Z]+)\\['(.*?)'(,(?![A-Z]+\\[)(.*?)\\])?");

    public void extractXmlFields(AssetBuilder assetBuilder) {

        String xmlFile = assetBuilder.getAbsolutePath() + ".xml";

        try {
            File file = new File(xmlFile);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);
            NodeList nodes = document.getElementsByTagName("Process");

            List<Map<String, Object>> lineage = Lists.newArrayList();
            assetBuilder.setAttr(attr(namespace, "lineage"), lineage);

            for (int i=0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                NamedNodeMap attrs = node.getAttributes();
                if (attrs == null) {
                    continue;
                }

                Map<String, Object> process = Maps.newHashMap();
                lineage.add(process);
                for (int j=0; j<attrs.getLength(); j++) {
                    process.put(attrs.item(j).getNodeName(), attrs.item(j).getNodeValue());
                }

                // Handle body of Process tag
                Matcher matcher = PARAM_PATTERN.matcher(node.getTextContent());
                while(matcher.find()) {

                    String type = matcher.group(1);
                    String key = matcher.group(2);
                    String value = matcher.group(4);

                    if (value == null) {
                        process.put(type, key);
                        if (!StringUtil.isNumeric(key)) {
                            assetBuilder.addKeywords(1, true, key);
                        }
                    }
                    else {
                        if (StringUtil.isNumeric(value)) {
                            process.put(key, Double.valueOf(value));
                        }
                        else {
                            process.put(key, value);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Failed to open ArcGIS format file: {}", xmlFile, e);
        }
    }
}
