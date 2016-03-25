package com.zorroa.analyst.ingestors;

import com.zorroa.analyst.service.AnalyzeService;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.SkipIngestException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.SourceSchema;
import com.zorroa.archivist.sdk.util.FileUtils;
import com.zorroa.archivist.sdk.util.Json;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The FilePathIngestor can parse tokens out of the file name and set them as attributes.
 *
 * Example options supplied in JSON which match a filename with metadata.
 * {
 *     "matchers": [
 *          {
 *              "regex": "^.+/(\w+)_(\w+)_(\w+)_(\w+)_(\d+)\\.mov$",
 *              "attrs": ["project", "scene", "shot", "task", "version"]
 *          }
 *     ],
 *     "representations": [
 *          {
 *              "primary": "blend",
 *              "secondary": [ ]
 *          }
 *     ]
 * }
 *
 */
public class FilePathIngestor extends IngestProcessor {

    private FilePathIngestor.Options opts;

    @Override
    public void init() {
        if (opts == null) {
            opts = Json.Mapper.convertValue(getArgs(), Options.class);
            if (opts == null) {
                opts = new Options();
            }
        }
    }

    @Override
    public void process(AssetBuilder assetBuilder) {
        processPathMatches(assetBuilder);
        processRepresentations(assetBuilder);
    }

    /**
     * The processRepresentations function collapses files that represent a single asset
     * into a single asset with multiple representations.
     *
     * @param assetBuilder
     */
    private void processRepresentations(AssetBuilder assetBuilder) {
        /**
         * Check for files with the same name but different extension.
         */
        if (opts.representations == null || opts.representations.isEmpty()) {
            return;
        }

        /**
         * Need to do 2 checks here.
         *
         * If the extension matches a defined "primary" extension, then search for
         * and add secondary representations.
         *
         * If the file is not the primary but has a name that matches the primary then
         * a SkipIngestException is thrown, which causes the file to be skipped.
         *
         */
        for (RepresentationMatcher repr: opts.representations) {
            if (assetBuilder.getExtension().equals(repr.primary)) {

                for (File file : new File(assetBuilder.getDirectory()).listFiles()) {
                    // ignore self
                    if (file.equals(assetBuilder.getFile())) {
                        continue;
                    }

                    String prefix = assetBuilder.getBasename() + ".";
                    if (file.getName().startsWith(prefix)) {
                        if (repr.isSecondary(file)) {
                            logger.info("Adding secondary representation {}", file);
                            SourceSchema secSource = new SourceSchema(file);
                            try {
                                secSource.setType(AnalyzeService.Tika.detect(file));
                            } catch (IOException e) {
                                logger.warn("unable to determine source type: {}", file, e);
                            }
                            assetBuilder.getSource().addRepresentation(secSource);
                        }
                    }
                }
            }
            else {
                // check if the primary exists in the same dir, otherwise we are primary.
                String primary = assetBuilder.getBasename() + "." + repr.primary;
                if (new File(assetBuilder.getDirectory() + "/" + primary).exists()) {
                    throw new SkipIngestException("Path is a secondary source, skipping " + assetBuilder.getAbsolutePath());
                }
            }
        }
    }

    private void processPathMatches(AssetBuilder assetBuilder) {

        /*
         * If they add this ingestor with no options just add the file name
         * which was the old default, and the directory the file is in.
         */
        if (opts.matchers == null || opts.matchers.isEmpty()) {
            assetBuilder.addKeywords(1, true, assetBuilder.getFilename());
            assetBuilder.addKeywords(1, true, FileUtils.filename(assetBuilder.getDirectory()));
            return;
        }

        for (FilePathMatcher matcher: opts.matchers) {
            Matcher m = matcher.getPattern().matcher(assetBuilder.getAbsolutePath());
            if (m.find()) {

                int attrCount = matcher.attrs.size();
                if (attrCount != m.groupCount()) {
                    /*
                     * Could be a warning or exception but we'll just a assume its a non match.
                     * We could also stop this from happening.
                     */
                    logger.debug("The number of pattern groups {} in regex '{}' does not match the number of attrs {}",
                            m.groupCount(), matcher.regex, matcher.attrs.size());
                    continue;
                }

                for (int i = 0; i < matcher.attrs.size(); i++) {
                    assetBuilder.setAttr(matcher.attrs.get(i), m.group(i+1));
                    if (matcher.keywords) {
                        assetBuilder.addKeywords(1, matcher.suggest, m.group(i+1));
                    }
                }
            }
        }
    }

    public Options getOpts() {
        return opts;
    }

    public FilePathIngestor setOpts(Options opts) {
        this.opts = opts;
        return this;
    }

    public static class RepresentationMatcher {
        public String primary;
        public List<String> secondaries;

        public RepresentationMatcher() {

        }

        public RepresentationMatcher(String primary) {
            this.primary = primary;
        }

        public boolean isPrimary(File file) {
            return file.getName().endsWith(primary);
        }

        public boolean isSecondary(File file) {
            if (secondaries == null || secondaries.isEmpty()) {
                return true;
            }
            String path = file.getAbsolutePath();
            for (String suffix: secondaries) {
                if (path.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class FilePathMatcher {
        public String regex;
        public List<String> attrs;
        public boolean keywords = true;
        public boolean suggest = true;
        private Pattern pattern = null;

        public Pattern getPattern() {
            if (pattern == null) {
                pattern = Pattern.compile(regex);
            }
            return pattern;
        }
    }


    public static class Options {
        public List<FilePathMatcher> matchers;
        public List<RepresentationMatcher> representations;
    }
}
