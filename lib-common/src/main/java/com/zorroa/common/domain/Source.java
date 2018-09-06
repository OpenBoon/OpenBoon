package com.zorroa.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zorroa.common.schema.SourceSchema;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by chambers on 7/6/16.
 */
public class Source extends Document {

    public Source() { }

    public Source(Document document) {
        super(document);
    }

    public Source(File file) {
        this(file, new HashMap());
    }

    public Source(Path file) {
        this(file.toFile(), new HashMap());
    }

    public Source(File file, Map<String, Object> attrs) {
        SourceSchema source = new SourceSchema(file);
        setAttr("source", source);

        for (Map.Entry<String, Object> attr: attrs.entrySet()) {
            setAttr(attr.getKey(), attr.getValue());
        }

        setId(IdGen.INSTANCE.getId(this));
    }

    public void addToKeywords(String namespace, Collection<String> words) {
        addToAttr(namespace + ".keywords",  words);
    }

    public void addToKeywords(String namespace, String ... words) {
        addToKeywords(namespace, Arrays.asList(words));
    }

    @JsonIgnore
    public SourceSchema getSourceSchema() {
        return getAttr("source", SourceSchema.class);
    }

    @JsonIgnore
    public Path getPath() {
        return Paths.get(getAttr("source.path", String.class));
    }

    @JsonIgnore
    public boolean exists() {
        return Files.exists(getPath());
    }
}
