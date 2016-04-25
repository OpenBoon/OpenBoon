package com.zorroa.analyst.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.filesystem.ObjectFile;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.DocumentSchema;
import com.zorroa.archivist.sdk.util.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 1/2/16.
 */
public class PdfIngestor extends IngestProcessor {

    public PdfIngestor() {
        supportedFormats.add("pdf");
    }

    @Override
    public void process(AssetBuilder assetBuilder) {

        if (assetBuilder.attrExists("document") && !assetBuilder.isChanged()) {
            logger.debug("'document' schema already exists, skipping: {}", assetBuilder);
        }

        extractMetadata(assetBuilder);
        extractImages(assetBuilder);
    }

    public void extractMetadata(AssetBuilder assetBuilder) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();

        try {
            parser.parse(assetBuilder.getInputStream(), handler, metadata);
            DocumentSchema schema = new DocumentSchema();
            assetBuilder.setAttr("document", schema);
        } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException(
                    "Unable to extract PDF metadata from " + assetBuilder.getAbsolutePath(), e, getClass());
        }
    }

    public void extractImages(AssetBuilder assetBuilder) {

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
            assetBuilder.setImage(pages.get(0).convertToImage());

            for (PDPage page: pages) {
                PDResources resources = page.getResources();
                Map images = resources.getImages();
                if(images != null) {

                    for (Object imageKey: images.keySet()) {
                        String key = (String)imageKey;
                        PDXObjectImage image = (PDXObjectImage)images.get(key);

                        /*
                         * Grab an object file for what are basically new source
                         * images.
                         */
                        ObjectFile file = objectFileSystem.prepare(
                                "assets", assetBuilder.getId(), image.getSuffix(), key);
                        if (file.exists()) {
                            /*
                             * The object file's name is based on the original asset ID
                             * plus the name of the image in the PDF.
                             */
                            logger.debug("file exists {} {}", key, image.getSuffix());
                            continue;
                        }
                        assetBuilder.getLinks().addToDerived(file.getFile().getAbsolutePath());
                        image.write2file(file.getFile());
                    }
                }
            }

        } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException(
                    "Unable to extract PDF image from " + assetBuilder.getAbsolutePath(), e, getClass());
        }
        finally {
            FileUtils.close(document);
        }
    }
}
