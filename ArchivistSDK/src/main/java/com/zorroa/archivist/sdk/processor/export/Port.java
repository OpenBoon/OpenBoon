package com.zorroa.archivist.sdk.processor.export;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by chambers on 11/5/15.
 */
public class Port<T> {

    private static final Logger logger = LoggerFactory.getLogger(Port.class);

    private final ExportProcessor parent;

    private String name;
    private Class<?> type;
    private List<T> values = null;
    private List<T> def = null;

    protected List<Socket<T>> sockets = new ArrayList();

    protected List<Cord<T>> cords = new ArrayList();

    public Port(String name, ExportProcessor parent) {
        this.name = name;
        this.parent = parent;
    }

    public Port(String name, T def, ExportProcessor parent) {
        this.name = name;
        this.def = Lists.newArrayList(def);
        this.parent = parent;
    }

    public String toString() {
        return String.format("<Port: name=%s Processor: %s", name, parent.getClass().getCanonicalName());
    }

    public void reset() {
        values = null;;
    }

    public T getValue() {
        if (values == null && sockets.isEmpty()) {
            if (def == null) {
                return null;
            }
            else {
                return handleAssetExpressions(this.def).get(0);
            }
        }
        else {
            return handleAssetExpressions(values.get(0));
        }
    }

    public T getValue(T tdef) {
        if (values == null && sockets.isEmpty()) {
            return handleAssetExpressions(tdef);
        }
        else {
            return handleAssetExpressions(values.get(0));
        }
    }

    public List<T> getValues() {
        if (values == null) {
            if (sockets.isEmpty()) {
                if (def == null) {
                    return null;
                } else {
                    return handleAssetExpressions(def);
                }
            }
            List<T> result = Lists.newArrayListWithCapacity(sockets.size());
            for (Socket<T> socket: sockets) {
                result.addAll(socket.getCord().getPort().getValues());
            }
            return result;
        }
        else {
            return values.stream().map(value-> handleAssetExpressions(value)).collect(Collectors.toList());
        }
    }

    public Port<T> setValue(T value) {
        this.values = Lists.newArrayList(value);
        return this;
    }

    public void addValue(T value) {
        if (this.values == null) {
            this.values = Lists.newArrayList();
        }
        this.values.add(value);
    }

    public Cord<T> cord() {
        Cord<T> cord = new Cord<>(this);
        cords.add(cord);
        return cord;
    }

    public Socket<T> socket() {
        Socket<T> socket =  new Socket<>(this);
        sockets.add(socket);
        return socket;
    }

    private final Pattern EXP_PATTERN = Pattern.compile("%\\{(.*?)\\}");

    private List<T> handleAssetExpressions(List<T> values) {
        List<T> result = Lists.newArrayListWithCapacity(values.size());
        for (T value: values) {
            result.add(handleAssetExpressions(value));
        }
        return result;
    }

    private T handleAssetExpressions(T value) {
        if (value == null) {
            return value;
        }
        if (!(value instanceof String)) {
            return value;
        }

        try {
            String sValue = (String) value;

            StringBuffer buffer = new StringBuffer();
            Matcher matcher = EXP_PATTERN.matcher(sValue);
            while(matcher.find()) {
                String exp = matcher.group(1);
                logger.info("Exp: {}={}", exp, getParent().getAsset().getValue(exp));
                matcher.appendReplacement(buffer, getParent().getAsset().getValue(exp));
            }
            matcher.appendTail(buffer);
            return (T) buffer.toString();

        } catch (ClassCastException e) {
        }

        return value;
    }

    public List<Cord<T>> getCords() {
        return cords;
    }

    public List<Socket<T>> getSockets() {
        return sockets;
    }

    public ExportProcessor getParent() {
        return parent;
    }
}
