package com.zorroa.archivist.sdk.domain;

/**
 * Convenience utility for creating attributes.
 */
public class Attr {

    public static final String DELIMITER = ":";

    /**
     * A convenience method which takes a variable list of strings and
     * turns it into an attribute name.  This is preferred over using
     * string concatenation.
     *
     * @param name
     * @return
     */
    public static final String attr(String... name) {
        return String.join(DELIMITER, name);
    }

    /**
     * Return the last part of an attribute string.  For example, if fully qualified
     * name is "a:b:c:d", this method will return "d".
     *
     * @param attr
     * @return
     */
    public static final String name(String attr) {
        return attr.substring(attr.lastIndexOf(DELIMITER) + 1);
    }

    /**
     * Return the fully qualified namespace for the attribute.  For example, if
     * the attribute is "a:b:c:d", this method will return "a:b:c"
     *
     * @param attr
     * @return
     */
    public static final String namespace(String attr) {
        return attr.substring(0, attr.lastIndexOf(DELIMITER));
    }
}

