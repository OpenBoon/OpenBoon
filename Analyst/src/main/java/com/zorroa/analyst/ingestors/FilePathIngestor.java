package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.util.FileUtils;
import com.zorroa.archivist.sdk.util.Json;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The FilePathIngestor can parse tokens out of the file name and set them as attributes.
 *
 * Example options supplied in JSON which match a filename with metadata.
 * {
 *     "match": [
 *          {
 *              "regex": "^.+/(\w+)_(\w+)_(\w+)_(\w+)_(\d+)\\.mov$",
 *              "attrs": ["project", "scene", "shot", "task", "version"]
 *          }
 *     ]
 * }
 *
 */
public class FilePathIngestor extends IngestProcessor {

    private FilePathIngestor.Options opts;

    @Override
    public void init() {
        opts = Json.Mapper.convertValue(getArgs(), Options.class);
    }

    @Override
    public void process(AssetBuilder assetBuilder) {

        /*
         * If they add this ingestor with no options just add the file name
         * which was the old default, and the directory the file is in.
         */
        if (opts.match.isEmpty()) {
            assetBuilder.addKeywords(1, true, assetBuilder.getFilename());
            assetBuilder.addKeywords(1, true, FileUtils.filename(assetBuilder.getDirectory()));
            return;
        }

        for (FilePathMatcher matcher: opts.match) {
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
        public List<FilePathMatcher> match = Lists.newArrayList();
    }
}
