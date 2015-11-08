package com.zorroa.archivist.sdk.processor.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Port defines a conduit for which data enters and leaves.  Data enters a port through
 * a socket and leaves through a Cord.
 */
public class Port<T> {

    public enum Type {
        Input,
        Output
    }

    private static final Logger logger = LoggerFactory.getLogger(Port.class);

    private final ExportProcessor parent;

    private String name;
    private Port.Type type;
    private List<T> values = null;
    private List<T> def = null;

    protected List<Socket<T>> sockets = new ArrayList();
    protected List<Cord<T>> cords = new ArrayList();

    public Port(String name, Port.Type type, ExportProcessor parent) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        parent.addPort(this, type);
    }

    public Port(String name, T def, Port.Type type, ExportProcessor parent) {
        this.name = name;
        this.type = type;
        this.def = Lists.newArrayList(def);
        this.parent = parent;
        parent.addPort(this, type);
    }

    public String toString() {
        return String.format("<Port: name=%s type=%s Processor: %s", name, parent.getClass().getCanonicalName());
    }

    public void setDefault(T value) {
        this.def = Lists.newArrayList(value);
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

    private final List<Pattern> EXPRESSION_PATTERNS = ImmutableList.<Pattern>builder()
            .add(Pattern.compile("%\\{(env):(.*?)\\}"))
            .add(Pattern.compile("%\\{(export):(.*?)\\}"))
            .add(Pattern.compile("(%)\\{(.*?)\\}"))
            .build();

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
            StringBuffer buffer = null;

            for (Pattern pattern: EXPRESSION_PATTERNS) {

                buffer = new StringBuffer();
                Matcher matcher = pattern.matcher(sValue);

                while(matcher.find()) {
                    String type = matcher.group(1);
                    String expr = matcher.group(2);

                    switch (type) {
                        case "%":
                            matcher.appendReplacement(buffer, getParent().getAsset().getValue(expr));
                            break;
                        case "env":
                            matcher.appendReplacement(buffer, System.getenv(expr.toUpperCase()));
                            break;
                        case "export":
                            try {
                                matcher.appendReplacement(buffer, String.valueOf(
                                        getParent().getExport().getClass()
                                        .getMethod("get" + StringUtil.capitalize(expr))
                                                .invoke(getParent().getExport())));
                            }
                            catch (Exception e) {
                                logger.warn("Failed to handle export expression: {}", expr, e);
                            }
                            break;
                        default:
                            throw new RuntimeException(String.format(
                                    "Invalid expression, not supported: '%s'", sValue));
                    }
                }


                matcher.appendTail(buffer);
                sValue = buffer.toString();
            }

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
