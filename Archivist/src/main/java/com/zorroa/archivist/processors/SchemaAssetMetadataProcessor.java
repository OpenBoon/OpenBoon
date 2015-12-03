package com.zorroa.archivist.processors;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ImageSchema;
import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 *
 * Extracts metadata from all types of supported files.
 * Base data that we create is added to the "source" namespace.
 *
 * @author chambers
 *
 */
public class SchemaAssetMetadataProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SchemaAssetMetadataProcessor.class);

    public SchemaAssetMetadataProcessor() { }

    /**
     * Extract asset metadata. Copy some fields to the default search
     * and suggestion field, and create a "source" namespace with dimensions,
     * location, and date information if available.
     *
     * Information extracted from the file headers are added to namespaces
     * such as "Exif", "IPTC" and others. Field and namespaces consist of
     * [A-Za-z0-9] characters, with no spaces, dashes or other characters.
     *
     * Arguments to this processor allow you to specify an ordered list
     * of fields to search for a master date field (dateTags=[String]) and
     * a list of fields to copy to the default search and suggestion field
     * (keywordTags=[String]). The components of these fields are specified
     * as, e.g., ["Exif.UserComment", "IPTC.Keywords"].
     *
     * @param asset
     */
    @Override
    public void process(AssetBuilder asset) {
        switch(asset.getSource().getType()) {
            case Image:
                extractImageData(asset);
                break;
            default:
                return;
        }
    }

    /**
     * Handles pulling metadata out of the image itself, either by
     * EXIF, EXR header, DPX header, etc.  Currently only supports
     * EXIF.
     *
     * @param asset
     */
    public void extractImageData(AssetBuilder asset) {
        /*
         * Extract all metadata fields into the format <directory>:<tag>=<value>,
         * and store two versions of the value, the original value, and an optional
         * .description variant which is more human-readable. Tag names are stored
         * using a string containing only [A-Za-a0-9] characters, with spaces, dashes
         * and other characters removed. This results in the EXIF-standard names,
         * though some formats (e.g. IPTC) contain tags with '/' or '-', (ugh).
         * Some tags have multiple names, which we'll handle later.
         */
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(asset.getFile());
            extractExifMetadata(asset, metadata);   // Extract all useful metadata fields in raw & descriptive format
            extractExifLocation(asset, metadata);   // Find the best location value and promote to top-level
        } catch (Exception e) {
            logger.error("Failed to load metadata: " + e.getMessage());
            asset.put("errors", "SchemaAssetMetadataProcessor", e.getMessage());
        }
    }

    /**
     * The default set of tags for building the keyword list.
     */
    private static final Set<String> defaultKeywordTags = ImmutableSet.<String>builder()
            .add("Exif.UserComment")
            .add("Exif.ColorSpace")
            .add("Exif.Make")
            .add("Exif.Model")
            .add("IPTC.Keywords")
            .add("IPTC.CopyrightNotice")
            .add("IPTC.Source")
            .add("IPTC.City")
            .add("IPTC.ProvinceState")
            .add("IPTC.CountryPrimaryLocationName")
            .add("File.Filename")
            .add("Xmp.Lens")
            .build();

    private static final List<String> defaultDateArgs = ImmutableList.<String>builder()
            .add("Exif.DateTimeOriginal")
            .add("Exif.DateTimeDigitized")
            .add("Exif.DateTime")
            .add("IPTC.DateCreated")
            .add("IPTC.TimeCreated")
            .add("File.FileModifiedDate")
            .build();

    private static final DateTimeFormatter extraDateFormatter = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss");

    private void extractExifMetadata(AssetBuilder asset, Metadata metadata) {

        Set<String> keywordArgs = getArgs().containsKey("keywordTags") ?
            ImmutableSet.copyOf((List<String>) getArgs().get("keywordTags")) : defaultKeywordTags;

        List<String> dateArgs = getArgs().containsKey("dateTags") ?
                (List<String>)getArgs().get("dateTags") : defaultDateArgs;

        /**
         * Set our mostValidDateField to a value outside the possible range.
         * If we find a date field less than this value we use it.  If we then
         * find one even further up the list, we use that.
         */
        int mostValidDateField = defaultDateArgs.size() + 1;

        for (Directory directory : metadata.getDirectories()) {
            String namespace = directory.getName().split(" ", 2)[0];
            for (Tag tag : directory.getTags()) {
                Object value = directory.getObject(tag.getTagType());
                if (value == null) {
                    continue;
                }
                String key = tag.getTagName().replaceAll("[^A-Za-z0-9]", "");
                String id = namespace + "." + key;

                /*
                 * Handle string formatted dates
                 */
                Date date = directory.getDate(tag.getTagType());
                if (date == null && value instanceof String) {
                    try {
                        DateTime dt = extraDateFormatter.parseDateTime((String)value);
                        date = dt.toDate();
                    } catch (IllegalArgumentException e) {
                        /*
                         * It wasn't a date, just ignore
                         */
                    }
                }

                /*
                 * Check for the most valid date.
                 */
                if (date != null) {
                    int dateFieldPriority = dateArgs.indexOf(id);
                    if (dateFieldPriority >= 0 && dateFieldPriority < mostValidDateField) {
                        mostValidDateField = dateFieldPriority;
                        asset.getSource().setDate(date);
                        continue;
                    }
                }

                /*
                 * Check for special data types that need to be handled, otherwise
                 * just add the data to the object
                 */
                if (value instanceof String) {
                    asset.setAttr(namespace, key, (String)value);
                    asset.addKeywords(keywordArgs.contains(id) ? KeywordsSchema.CONFIDENCE_MAX : 0, true, (String)value);
                } else if (value instanceof Rational) {
                    Rational rational = (Rational)value;
                    asset.setAttr(namespace, key, rational.doubleValue());
                } else if (value.getClass().isArray()) {
                    String componentName = value.getClass().getComponentType().getName();
                    if (componentName.equals("java.lang.String")) {
                        String[] strList = (String[])value;
                        asset.setAttr(namespace, key, value);
                        asset.addKeywords(keywordArgs.contains(id) ? KeywordsSchema.CONFIDENCE_MAX : 0, true, strList);
                    } else if (componentName.equals("com.drew.lang.Rational")) {
                        Rational[] rationals = (Rational[]) value;
                        Double[] doubles = new Double[rationals.length];
                        for (int i = 0; i < rationals.length; i++) {
                            doubles[i] = rationals[i].doubleValue();
                        }
                        asset.setAttr(namespace, key, doubles);
                    } else if (componentName.equals("byte") && Array.getLength(value) <= 16) {
                        asset.setAttr(namespace, key, value);
                    } else {
                        asset.setAttr(namespace, key, value);
                    }
                } else {
                    asset.setAttr(namespace, key, value);
                }
            }
        }
    }

    private static double dmsToDegrees(int d, int m, int s) {
        return Math.signum(d) * (Math.abs(d) + (m / 60.0) + (s / 3600.0));
    }

    private void extractExifLocation(AssetBuilder asset, Metadata metadata) {
        Directory exifDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (exifDirectory != null) {
            int[] latitude = exifDirectory.getIntArray(GpsDirectory.TAG_LATITUDE);
            int[] longitude = exifDirectory.getIntArray(GpsDirectory.TAG_LONGITUDE);
            if (latitude != null && longitude != null) {
                double lat = dmsToDegrees(latitude[0], latitude[1], latitude[2]);
                double lon = dmsToDegrees(longitude[0], longitude[1], longitude[2]);
                Point2D.Double location = new Point2D.Double(lat, lon);
                asset.getSchema("image", ImageSchema.class).setLocation(location);
            }
        }
    }
}
