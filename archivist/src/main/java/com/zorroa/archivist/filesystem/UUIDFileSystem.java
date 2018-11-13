package com.zorroa.archivist.filesystem;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simple UUID/Object based file system.
 */
public class UUIDFileSystem extends AbstractFileSystem implements ObjectFileSystem {

    private static final int DEEPNESS = 7;

    private static final NameBasedGenerator nameBasedGenerator =
            Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL);

    private String baseDir;
    private File root;

    public UUIDFileSystem(File root) {
        this.root = root;
    }

    @Override
    public void init() {
        root.mkdirs();
        baseDir = root.toPath().normalize().toAbsolutePath().toString();
        logger.info("OFS Filesystem initialized: {}", baseDir);
    }

    @Override
    public OfsFile prepare(String category, Object value, String type, List<String> variants) {
        OfsFile file = get(category, value, type, variants);
        file.mkdirs();
        return file;
    }

    @Override
    public OfsFile get(String category, Object value, String type, List<String> variants) {
        UUID uuid = nameBasedGenerator.generate(value.toString());
        StringBuilder sb = getParentDirectory(category, uuid);
        String name = getFilename(uuid, type, variants);
        sb.append(name);

        return new OfsFile(category, name, new File(sb.toString()));
    }

    private static final Pattern REGEX_NAME =
            Pattern.compile("^([a-f0-9\\-]{36})_?([_\\w]+)?\\.([\\w]+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public OfsFile get(String category, String name) {
        Matcher matcher = REGEX_NAME.matcher(name);
        if (matcher.matches()) {
            UUID id = UUID.fromString(matcher.group(1));
            String ext = matcher.group(3);
            StringBuilder sb = getParentDirectory(category, id);

            String variant = matcher.group(2);
            if (variant != null) {
                sb.append(getFilename(id, ext, Lists.newArrayList(variant)));
            }
            else {
                sb.append(getFilename(id, ext, null));
            }

            return new OfsFile(category, name, new File(sb.toString()));
        }
        else {
            throw new IllegalArgumentException("Invalid object ID: " + name);
        }
    }

    @Override
    public OfsFile get(String id) {
        id = id.replace("ofs://", "");
        String e[] = id.split("/", 2);
        return get(e[0], e[1]);
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

    private String getFilename(UUID id, String type, List<String> variants) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(id);
        if (variants != null && !variants.isEmpty()) {
            sb.append("_");
            sb.append(String.join("_", variants));
        }
        sb.append(".");
        sb.append(type);
        return sb.toString();
    }
}
