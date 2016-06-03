package com.zorroa.archivist.crawlers;

import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.zorroa.fileseq.FileSequence;
import com.zorroa.fileseq.Fileseq;
import com.zorroa.sdk.domain.AnalyzeRequestEntry;
import com.zorroa.sdk.exception.AbortCrawlerException;
import com.zorroa.sdk.processor.Crawler;
import com.zorroa.sdk.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * Created by chambers on 2/1/16.
 */
public class FileCrawler extends Crawler {

    final static Logger logger = LoggerFactory.getLogger(FileCrawler.class);

    /**
     * Minimum number of frames for a set of files to be considered a sequence.
     */
    public final int MIN_SEQUENCE_FRAMES = 2;

    private final SequenceManager sequenceManager = new SequenceManager();

    @Override
    public void start(URI uri, Consumer<AnalyzeRequestEntry> consumer) throws IOException {
        final LongAdder total = new LongAdder();
        Path start = new File(uri).toPath();

        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                        throws IOException {
                    final File file = path.toFile();

                    if (!file.isFile()) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (path.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (!targetFileFormats.contains(FileUtils.extension(path).toLowerCase())
                            && !targetFileFormats.isEmpty()) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (ignoredPaths.contains(file.getAbsolutePath())) {
                        return FileVisitResult.CONTINUE;
                    }

                    FileSequence fs = new FileSequence(file.getAbsolutePath());
                    String range = fs.getRange();
                    if (fs.isValid() && Ints.tryParse(range) != null) {
                        fs.setAttr(1, "firstFrame", file.getAbsolutePath());
                        fs.setRange("");
                        try {
                            sequenceManager.addFrame(fs, Integer.valueOf(range), fs.getZfill());
                        } catch (Exception e) {
                            logger.warn("Failed to handle file: {}", path, e);
                        }
                    } else {
                        try {
                            consumer.accept(new AnalyzeRequestEntry(file.toURI()));
                            total.increment();
                        } catch (AbortCrawlerException e) {
                            return FileVisitResult.TERMINATE;
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                        throws IOException {
                    final String path = dir.toString();

                    Map<FileSequence, TreeSet<Integer>> sequences = sequenceManager.get(path);
                    if (sequences == null) {
                        return FileVisitResult.CONTINUE;
                    }

                    sequenceManager.remove(path);

                    for (Map.Entry<FileSequence, TreeSet<Integer>> entry: sequences.entrySet()) {

                        FileSequence fileseq = entry.getKey();
                        TreeSet<Integer> frames = entry.getValue();

                        /*
                         * The first frame of the sequence gets submitted because otherwise
                         * things can go wrong if the file doesn't exist.
                         */

                        String asset = (String) fileseq.getAttrs(1).get("firstFrame");
                        /*
                         * We have to have more than 1 frame to be considered a sequence.
                         */
                        try {
                            if (frames.size() >= MIN_SEQUENCE_FRAMES) {
                                AnalyzeRequestEntry req = new AnalyzeRequestEntry(asset);
                                req.setAttr("sequence.spec", fileseq.getFileSpec());
                                req.setAttr("sequence.range", Fileseq.framesToFrameRange(frames, fileseq.getZfill()));
                                req.setAttr("sequence.zfill", fileseq.getZfill());
                                req.setAttr("sequence.padding", fileseq.getPadding());
                                req.setAttr("sequence.start", frames.first());
                                req.setAttr("sequence.end", frames.last());
                                req.setAttr("sequence.size", frames.size());
                                consumer.accept(req);
                            } else {
                                for (int frame : frames) {
                                    consumer.accept(new AnalyzeRequestEntry(fileseq.getFrame(frame)));
                                }
                            }
                            total.increment();
                        } catch (AbortCrawlerException xe) {
                            return FileVisitResult.TERMINATE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            logger.warn("Failed to walk path: {}", start, e);
            throw e;
        }

        logger.info("Total files submitted: {}", total.longValue());
    }

    /**
     * Keeps track of any sequences we find, along with their frames.
     */
    private static class SequenceManager {

        private final Map<String, Map<FileSequence, TreeSet<Integer>>> sequences = Maps.newHashMap();

        public Map<FileSequence, TreeSet<Integer>> get(String dir) {
            return sequences.get(dir);
        }

        public void remove(String dir) {
            sequences.remove(dir);
        }

        public void addFrame(FileSequence fileseq, int frame, int padding) {
            String dir = FileUtils.dirname(fileseq.getFileSpec());

            Map<FileSequence, TreeSet<Integer>> dirSeqs = sequences.get(dir);
            if (dirSeqs == null) {
                dirSeqs = Maps.newHashMap();
                sequences.put(dir, dirSeqs);
            }

            TreeSet<Integer> frames = dirSeqs.get(fileseq);
            if (frames == null) {
                frames = new TreeSet<>();
                dirSeqs.put(fileseq, frames);
            }

            frames.add(frame);
        }
    }
}
