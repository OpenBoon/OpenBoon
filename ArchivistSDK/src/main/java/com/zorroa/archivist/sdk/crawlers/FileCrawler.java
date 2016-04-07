package com.zorroa.archivist.sdk.crawlers;

import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.AnalyzeRequestEntry;
import com.zorroa.archivist.sdk.util.FileUtils;
import com.zorroa.fileseq.FileSequence;
import com.zorroa.fileseq.Fileseq;
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
import java.util.function.Consumer;

/**
 * Created by chambers on 2/1/16.
 */
public class FileCrawler extends AbstractCrawler {

    final static Logger logger = LoggerFactory.getLogger(FileCrawler.class);

    private final SequenceManager sequenceManager = new SequenceManager();

    @Override
    public void start(URI uri, Consumer<AnalyzeRequestEntry> consumer) throws IOException {
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
                    if (fs.isValid()) {
                        String frame = fs.getRange();
                        fs.setRange("");
                        sequenceManager.addFrame(fs,
                                Integer.valueOf(frame),fs.getZfill());
                    }
                    else {
                        consumer.accept(new AnalyzeRequestEntry(file.toURI()));
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
                        AnalyzeRequestEntry req = new AnalyzeRequestEntry(fileseq.getFrame(frames.first()));

                        /*
                         * We have to have more than 1 frame to be considered a sequence.
                         */
                        if (frames.size() > 1) {
                            req.setAttr("sequence.spec", fileseq.getFileSpec());
                            req.setAttr("sequence.range", Fileseq.framesToFrameRange(frames, fileseq.getZfill()));
                            req.setAttr("sequence.zfill", fileseq.getZfill());
                            req.setAttr("sequence.padding", fileseq.getPadding());
                            req.setAttr("sequence.start", frames.first());
                            req.setAttr("sequence.end", frames.last());
                            req.setAttr("sequence.size", frames.size());
                        }

                        consumer.accept(req);
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            logger.warn("Failed to walk path: {}", start, e);
            throw e;
        }
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
