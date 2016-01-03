package com.zorroa.archivist.processors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.DocumentSchema;
import com.zorroa.archivist.sdk.service.EventLogService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by chambers on 1/2/16.
 */
public class PdfIngestor extends IngestProcessor {

    @Autowired
    EventLogService eventLogService;

    public PdfIngestor() {
        supportedFormats.add("pdf");
    }

    @Override
    public void process(AssetBuilder assetBuilder) {
        extractMetadata(assetBuilder);
        extractImage(assetBuilder);
    }

    public void extractMetadata(AssetBuilder assetBuilder) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();

        try {
            parser.parse(assetBuilder.getInputStream(), handler, metadata);
            DocumentSchema schema = new DocumentSchema();
            schema.setAuthor(metadata.get("Author"));
            schema.setTitle(metadata.get("title"));
            schema.setPages(Integer.valueOf(metadata.get("xmpTPg:NPages")));
            assetBuilder.getSource().setDate(metadata.getDate(Property.get("Last-Save-Date")));

        } catch (Exception e) {
            eventLogService.log(
                    "Failed to extract metadata from PDF: {}", e, assetBuilder.getAbsolutePath());
        }
    }

    public void extractImage(AssetBuilder assetBuilder) {
        try {
            PDDocument document = PDDocument.load(assetBuilder.getInputStream());
            if (document.isEncrypted()) {
                try {
                    document.decrypt("");
                    document.setAllSecurityToBeRemoved(true);
                }
                catch (Exception e) {
                    throw new Exception("The document is encrypted, and we can't decrypt it.", e);
                }
            }

            List<PDPage> pages = document.getDocumentCatalog().getAllPages();
            PDPage page = pages.get(0);
            assetBuilder.setImage(page.convertToImage());
        } catch (Exception e) {
            eventLogService.log(
                    "Failed to extract image from PDF: {}", e, assetBuilder.getAbsolutePath());
        }
    }
}
