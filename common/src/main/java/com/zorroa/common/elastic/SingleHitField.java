package com.zorroa.common.elastic;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.search.SearchHitField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class SingleHitField implements SearchHitField {

    private static final Logger logger = LoggerFactory.getLogger(SingleHitField.class);

    private final GetField field;

    public SingleHitField(GetField field) {
        this.field = field;
    }
    @Override
    public String name() {
        return field == null ? null : field.getName();
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public <V> V value() {
        return field == null ? null : (V) field.getValue();
    }

    @Override
    public <V> V getValue() {
        return value();
    }

    @Override
    public List<Object> values() {
        return field == null ? null : field.getValues();
    }

    @Override
    public List<Object> getValues() {
        return values();
    }

    @Override
    public boolean isMetadataField() {
        return false;
    }

    @Override
    public Iterator<Object> iterator() {
        return null;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {

    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {

    }
}
