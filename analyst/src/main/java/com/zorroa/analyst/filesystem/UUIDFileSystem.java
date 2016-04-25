package com.zorroa.analyst.filesystem;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.zorroa.analyst.service.TransferService;
import com.zorroa.archivist.sdk.exception.FileSystemException;
import com.zorroa.archivist.sdk.filesystem.AbstractFileSystem;
import com.zorroa.archivist.sdk.filesystem.ObjectFile;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simple UUID/Object based file system.
 */
public class UUIDFileSystem extends AbstractFileSystem {

    private static final int DEEPNESS = 7;

    private static final NameBasedGenerator nameBasedGenerator = Generators.nameBasedGenerator();

    private String baseDir;

    public UUIDFileSystem(Properties props) {
        super(props);
    }

    @Autowired
    TransferService transferService;

    @Override
    public void init() {
        File directory = new File(properties.getProperty("root"));
        directory.mkdirs();

        baseDir = directory.toPath().normalize().toAbsolutePath().toString();
        logger.info("Filesystem initialized: {}", baseDir);
    }

    @Override
    public ObjectFile prepare(String category, Object id, String type, String ... variant) {
        UUID uuid = nameBasedGenerator.generate(id.toString());
        StringBuilder sb = getParentDirectory(category, uuid);
        sb.append(getFilename(uuid, type, variant));
        ObjectFile file = new ObjectFile(category, sb.toString());
        file.mkdirs();
        return file;
    }

    private static final Pattern REGEX_NAME =
            Pattern.compile("^([a-f0-9\\-]{36})_?([_\\w]+)?\\.([\\w]+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public ObjectFile get(String category, String name) {
        Matcher matcher = REGEX_NAME.matcher(name);
        if (matcher.matches()) {
            UUID id = UUID.fromString(matcher.group(1));
            String variant = matcher.group(2);
            String ext = matcher.group(3);

            StringBuilder sb = getParentDirectory(category, id);
            sb.append(getFilename(id, ext, variant));

            return new ObjectFile(category, sb.toString());
        }
        else {
            throw new IllegalArgumentException("Invalid object ID: " + name);
        }
    }

    @Override
    public ObjectFile get(String id) {
        String e[] = id.split("/", 2);
        return get(e[0], e[1]);
    }

    @Override
    public ObjectFile transfer(URI src, ObjectFile file) {
        if (!file.exists()) {
            try {
                transferService.transfer(src, file);
            } catch (IOException e) {
                throw new FileSystemException(e);
            }
        }
        return file;
    }
    /**
     * Return the parent directory for the given category and unique ID.
     *
     * @param category
     * @param id
     * @return
     */
    private StringBuilder getParentDirectory(String category, UUID id) {
        String _id = id.toString();
        StringBuilder sb = new StringBuilder(256);
        sb.append(baseDir);
        sb.append("/");
        sb.append(category);
        sb.append("/");
        for (int i=0; i<=DEEPNESS; i++) {
            sb.append(_id.charAt(i));
            sb.append("/");
        }
        return sb;
    }

    private StringBuilder getFilename(UUID id, String type, String ... variant) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(id);
        if (variant.length > 0) {
            sb.append("_");
            sb.append(String.join("_", variant));
        }
        sb.append(".");
        sb.append(type);
        return sb;
    }

    @Override
    public String getUrl(ObjectFile file) {
        Path abs = file.getFile().toPath();
        Path base = new File(baseDir).toPath();

        StringBuilder url = new StringBuilder(128);
        url.append(System.getProperty("server.url"));
        url.append("/api/v1/fs/");
        url.append(base.relativize(abs).toString());
        return url.toString();
    }

}
