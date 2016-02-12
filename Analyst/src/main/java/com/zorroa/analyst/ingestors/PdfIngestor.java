package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.DocumentSchema;
import com.zorroa.archivist.sdk.util.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;

import java.util.List;

/**
 * Created by chambers on 1/2/16.
 */
public class PdfIngestor extends IngestProcessor {

    public PdfIngestor() {
        supportedFormats.add("pdf");
    }

    @Override
    public void process(AssetBuilder assetBuilder) {

        if (assetBuilder.contains("document") && !assetBuilder.isChanged()) {
            logger.debug("'document' schema already exists, skipping: {}", assetBuilder);
        }

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
            /*
            schema.setAuthor(metadata.get("Author"));
            schema.setTitle(metadata.get("title"));
            schema.setPages(Integer.valueOf(metadata.get("xmpTPg:NPages")));
            assetBuilder.getSource().setDate(metadata.getDate(Property.get("Last-Save-Date")));
            */
            assetBuilder.addSchema(schema);
        } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException(
                    "Unable to extract PDF metadata from " + assetBuilder.getAbsolutePath(), e, getClass());
        }
    }

    public void extractImage(AssetBuilder assetBuilder) {

        PDDocument document = null;
        try {
            document = PDDocument.load(assetBuilder.getInputStream());
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
            throw new UnrecoverableIngestProcessorException(
                    "Unable to extract PDF image from " + assetBuilder.getAbsolutePath(), e, getClass());
        }
        finally {
            FileUtils.close(document);
        }
    }
}
