package com.zorroa.archivist.processors;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.AssetBuilder;
import com.zorroa.archivist.sdk.IngestProcessor;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * Extracts metadata from all types of supported files.
 * Base data that we create is added to the "source" namespace.
 *
 * @author chambers
 *
 */
public class AssetMetadataProcessor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AssetMetadataProcessor.class);

    public AssetMetadataProcessor() { }

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
        if (ingestProcessorService.isImage(asset)) {
            extractImageData(asset);
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
            extractMetadata(asset, metadata);   // Extract all useful metadata fields in raw & descriptive format
            extractDate(asset, metadata);       // Find the best date value and promote to top-level
            extractLocation(asset, metadata);   // Find the best location value and promote to top-level
        } catch (Exception e) {
            logger.error("Failed to load metadata: " + e.getMessage());
            asset.put("source", "error", "Invalid metadata");
        }
    }

    private void extractMetadata(AssetBuilder asset, Metadata metadata) {
        List<String> keywordArgs = (List<String>) getArgs().get("keywordTags");
        if (keywordArgs == null) {
            keywordArgs = new ArrayList<String>();
            keywordArgs.add("Exif.UserComment");
            keywordArgs.add("Exif.ColorSpace");
            keywordArgs.add("Exif.Make");
            keywordArgs.add("Exif.Model");
            keywordArgs.add("IPTC.Keywords");
            keywordArgs.add("IPTC.CopyrightNotice");
            keywordArgs.add("IPTC.Source");
            keywordArgs.add("IPTC.City");
            keywordArgs.add("IPTC.ProvinceState");
            keywordArgs.add("IPTC.CountryPrimaryLocationName");
            keywordArgs.add("File.Filename");
            keywordArgs.add("Xmp.Lens");
        }
        Set<String> idSet = new HashSet<String>(keywordArgs);
        DateTimeFormatter extraDateFormatter = DateTimeFormat.forPattern("yyyy:MM:dd:HH:mm:ss");

        for (Directory directory : metadata.getDirectories()) {
            String dirName = directory.getName().split(" ", 2)[0];
            for (Tag tag : directory.getTags()) {
                String tagName = tag.getTagName().replaceAll("[^A-Za-z0-9]", "");
                Object obj = directory.getObject(tag.getTagType());
                if (obj == null) {
                    continue;
                }

                Date date = directory.getDate(tag.getTagType());
                if (date == null && tagName.toLowerCase().contains("datetime") && obj instanceof String) {
                    try {
                        DateTime dt = extraDateFormatter.parseDateTime((String)obj);
                        date = dt.toDate();
                    } catch (IllegalArgumentException e) {
                        // Parsing error
                    }
                    if (date == null) {
                        // Skip the poorly formatted date field rather than generating parsing exception
                        continue;
                    }
                }
                String id = dirName + "." + tagName;
                if ((obj instanceof String && date == null) ||
                        (obj.getClass().isArray() && obj.getClass().getComponentType().getName().equals("java.lang.String"))) {
                    if (idSet.contains(id)) {
                        asset.map(dirName, tagName, "copy_to", null);
                    }
                    asset.map(dirName, tagName, "type", "string");
                    HashMap<String, String> raw = new HashMap<>(2);
                    raw.put("type", "string");
                    raw.put("index", "not_analyzed");
                    HashMap<String, Object> fields = new HashMap<>(2);
                    fields.put("raw", raw);
                    asset.map(dirName, tagName, "fields", fields);
                }

                if (obj.getClass().isArray()) {
                    String componentName = obj.getClass().getComponentType().getName();
                    if (componentName.equals("com.drew.lang.Rational")) {
                        Rational[] rationals = (Rational[]) obj;
                        Double[] doubles = new Double[rationals.length];
                        for (int i = 0; i < rationals.length; i++) {
                            doubles[i] = rationals[i].doubleValue();
                        }
                        obj = doubles;
                    } else if (componentName.equals("byte") && Array.getLength(obj) > 16) {
                        continue;       // Skip bigger byte arrays with binary data
                    }
                } else {
                    // Convert any object types that do not translate to JSON
                    if (date != null) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSS");
                        obj = dateFormat.format(date);
                        asset.map(dirName, tagName, "type", "date");
                    } else if (obj instanceof Rational) {
                        Rational rational = (Rational)obj;
                        obj = rational.doubleValue();
                    }
                }

                // Always save the raw format
                asset.put(dirName, tagName, obj);

                // Always save the raw format, and also save a description if it
                // has some useful additional information for searching & display
                String description = tag.getDescription();
                if (description != null &&
                        !description.equals(directory.getString(tag.getTagType())) &&
                        (!obj.getClass().isArray() || (!obj.getClass().getComponentType().getName().equals("java.lang.String") && Array.getLength(obj) <= 16))) {
                    String descTagName = tagName + ".description";
                    HashMap<String, String> raw = new HashMap<>(2);
                    raw.put("type", "string");
                    raw.put("index", "not_analyzed");
                    HashMap<String, Object> fields = new HashMap<>(2);
                    fields.put("raw", raw);
                    asset.map(dirName, descTagName, "fields", fields);
                    asset.map(dirName, descTagName, "type", "string");
                    asset.put(dirName, descTagName, description);
                    if (idSet.contains(id)) {
                        asset.map(dirName, descTagName, "copy_to", null);
                    }
                }
            }
        }
    }

    private class MetaField {
        public Directory directory;
        public Tag tag;

        public MetaField(Directory directory, Tag tag) {
            this.directory = directory;
            this.tag = tag;
        }
    }

    // Search metadata for <directory-name>.<tag-name>
    private MetaField tagForIdentifier(String id, Metadata metadata) {
        String names[] = id.toLowerCase().split("\\.");
        if (names.length != 2 || names[0] == null || names[1] == null) {
            return null;
        }
        for (Directory directory : metadata.getDirectories()) {
            String dirName = directory.getName().split(" ", 2)[0].toLowerCase();
            if (dirName.equals(names[0])) {
                Collection<Tag> tags = directory.getTags();
                for (Tag tag : tags) {
                    String tagName = tag.getTagName().replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                    if (tagName.equals(names[1])) {
                        return new MetaField(directory, tag);
                    }
                }
            }
        }
        return null;
    }

    private void extractDate(AssetBuilder asset, Metadata metadata) {
        // Get an ordered list of fields from the processor args, or use reasonable defaults
        List<String> dateArgs = (List<String>) getArgs().get("dateTags");
        if (dateArgs == null) {
            dateArgs = new ArrayList();
            dateArgs.add("Exif.DateTimeOriginal");
            dateArgs.add("Exif.DateTimeDigitized");
            dateArgs.add("Exif.DateTime");
            dateArgs.add("IPTC.DateCreated");       // TODO: Combine IPTC date+time fields
            dateArgs.add("IPTC.TimeCreated");
            dateArgs.add("File.FileModifiedDate");
        }

        // Run through the array of fields, optionally specified as an argument,
        // to determine which date field to promote to the global date
        Date date = null;
        for (String id : dateArgs) {
            MetaField field = tagForIdentifier(id, metadata);
            if (field == null)
                continue;

            // Convert from string, if necessary, to date
            // TODO: Combile IPTC date+time fields
            date = field.directory.getDate(field.tag.getTagType());
            if (date != null) {
                break;
            }
        }

        if (date != null) {
            asset.map("source", "date", "type", "date");
            asset.put("source", "date", date);
        }
    }

    private double dmsToDegrees(int d, int m, int s) {
        return Math.signum(d) * (Math.abs(d) + (m / 60.0) + (s / 3600.0));
    }

    private void extractLocation(AssetBuilder asset, Metadata metadata) {
        Directory exifDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (exifDirectory != null) {
            int[] latitude = exifDirectory.getIntArray(GpsDirectory.TAG_LATITUDE);
            int[] longitude = exifDirectory.getIntArray(GpsDirectory.TAG_LONGITUDE);
            if (latitude != null && longitude != null) {
                String latitudeRef = exifDirectory.getString(GpsDirectory.TAG_LATITUDE_REF);
                String longitudeRef = exifDirectory.getString(GpsDirectory.TAG_LONGITUDE_REF);
                double lat = dmsToDegrees(latitude[0], latitude[1], latitude[2]);
                double lon = dmsToDegrees(longitude[0], longitude[1], longitude[2]);
                Map<String, Object> geoPoint = Maps.newHashMapWithExpectedSize(2);
                geoPoint.put("lat", lat);
                geoPoint.put("lon", lon);
                asset.map("source", "location", "type", "geo_point");
                asset.put("source", "location", geoPoint);
            }
        }
    }
}
