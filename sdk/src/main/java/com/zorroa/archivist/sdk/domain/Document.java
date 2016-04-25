package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.schema.JsonAnyRemover;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.sdk.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A Document is a wrapper around a Map<String,Object> which provides a convenience interface
 * to the underlying structure.
 */
public class Document {

    private static final Logger logger = LoggerFactory.getLogger(Document.class);

    protected  Map<String, Object> document = Maps.newHashMap();

    public Map<String, Object> getDocument() {
        return document;
    }

    public void setDocument(Map<String, Object> data) {
        this.document = data;
    }

    @Override
    public String toString() {
        return Json.serializeToString(document);
    }

    /**
     * Get an attribute value  by its fully qualified name.
     *
     * @param attr
     * @param <T>
     * @return
     */
    public <T> T getAttr(String attr) {
        Object current = getContainer(attr, false);
        return (T) getChild(current, Attr.name(attr));
    }

    /**
     * Get an attribute value by its fully qualified name.  Uses a JSON mapper
     * to map the data into the specified class.
     *
     * @param attr
     * @param type
     * @param <T>
     * @return
     */
    public <T> T getAttr(String attr, Class<T> type) {
        Object current = getContainer(attr, false);
        return Json.Mapper.convertValue(getChild(current, Attr.name(attr)), type);
    }

    /**
     * Get an attibute value by its fully qualified name.  Uses a JSON mapper
     * to map the data into the specified TypeReference.
     *
     * @param attr
     * @param type
     * @param <T>
     * @return
     */
    public <T> T getAttr(String attr, TypeReference<T> type) {
        Object current = getContainer(attr, false);
        return Json.Mapper.convertValue(getChild(current, Attr.name(attr)), type);
    }

    /**
     * Assumes the target attribute is a collection of some sort and tries to add
     * the given value.
     *
     * @param attr
     * @param value
     */
    public void addToAttr(String attr, Object value) {
        Object current = getContainer(attr, true);
        String key = Attr.name(attr);

        try {
            current.getClass().getMethod("addTo"+ StringUtil.capitalize(key), value.getClass()).invoke(current, value);
        }
        catch (Exception e) {
            /**
             * Handle the JsonAnyGetter case.
             */
            for (Method m : current.getClass().getMethods()) {
                if (m.isAnnotationPresent(JsonAnyGetter.class)) {
                    try {
                        Map map = (Map) m.invoke(current);
                        Collection collection = (Collection)map.get(key);
                        if (collection == null) {
                            collection = (Collection) current.getClass().getMethod("getDefaultValue").invoke(current);
                            map.put(key, collection);
                        }

                        if (value instanceof Collection) {
                            collection.addAll((Collection) value);
                        }
                        else {
                            collection.add(value);
                        }
                        return;
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid any getter call: " + key + "," + ex, ex);
                    }
                }
            }

            /**
             * Handle the case where the object is a standard map.
             */
            try {
                Map map = ((Map) current);
                Collection collection = (Collection)map.get(key);
                if (collection == null) {
                    collection = Lists.newArrayList();
                    map.put(key, collection);
                }
                if (value instanceof Collection) {
                    collection.addAll((Collection) value);
                }
                else {
                    collection.add(value);
                }
            } catch (ClassCastException ex) {
                logger.info("what", ex);
                throw new IllegalArgumentException(
                        "The attribute is not a Collection, " + current.getClass().getName());
            }
        }
    }

    /**
     * Set an attribute value.
     *
     * @param attr
     * @param value
     */
    public void setAttr(String attr, Object value) {
        Object current = getContainer(attr, true);
        String key = Attr.name(attr);

        try {
            /*
             * Try to use an exposed setter method.
             */
            writeMethod(current, key).invoke(current, value);
            return;
        } catch (Exception e) {

            /*
             * If the setter doesn't exist, try to use the any setter.
             */
            for (Method m : current.getClass().getMethods()) {
                if (m.isAnnotationPresent(JsonAnySetter.class)) {

                    try {
                        m.invoke(current, key, value);
                        return;
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid any setter call: " + attr + " value: " + value, e);
                    }
                }
            }

            /*
             * Finally, try treating it like a map.
             */
            try {
                ((Map<String, Object>) current).put(key, value);
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Invalid attribute: " + attr, ex);
            }
        }
    }

    /**
     * Remove an attribute.  If the attr cannot be remove it is set to null.
     *
     * @param attr
     */
    public boolean removeAttr(String attr) {
        Object current = getContainer(attr, true);
        String key = Attr.name(attr);

        try {
            writeMethod(current, key).invoke(current, new Object[] { null });
            return true;
        } catch (Exception e) {
            /*
             * If the setter doesn't exist, try to use the any setter.
             */
            for (Method m : current.getClass().getMethods()) {
                if (m.isAnnotationPresent(JsonAnyRemover.class)) {
                    try {
                        return (Boolean) m.invoke(current, key);
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid any remove call: " + attr + "," + e, e);
                    }
                }
            }

            /*
             * Finally, just try treating it like a map.
             */
            try {
                return ((Map<String, Object>) current).remove(key) != null;
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Invalid attribute: " + attr);
            }
        }
    }

    /**
     * Return true if the document has the given namespace.
     * @param attr
     * @return
     */
    public boolean attrExists(String attr) {
        Object container = getContainer(attr, false);
        return getChild(container, Attr.name(attr)) != null;
    }

    /**
     * Return true if the value of an attribute contains the given value.
     *
     * @param attr
     * @return
     */
    public boolean attrContains(String attr, Object value) {
        Object parent = getContainer(attr, false);
        Object child = getChild(parent, Attr.name(attr));

        if (child instanceof Collection) {
            return ((Collection) child).contains(value);
        }
        else if (child instanceof String) {
            return ((String) child).contains(value.toString());
        }
        return false;
    }

    private static final Pattern PATTERN_ATTR = Pattern.compile(Attr.DELIMITER, Pattern.LITERAL);

    private Object getContainer(String attr, boolean forceExpand) {
        String[] parts = PATTERN_ATTR.split(attr);

        Object current = document;
        for (int i=0; i<parts.length-1; i++) {

            Object child = getChild(current, parts[i]);
            if (child == null) {
                if (forceExpand) {
                    child = createChild(current, parts[i]);
                }
                else {
                    return null;
                }
            }
            current = child;
        }
        return current;
    }

    private Object getChild(Object object, String key) {
        if (object == null) {
            return null;
        }
        try {
            return readMethod(object, key).invoke(object);
        } catch (Exception e) {
            /*
             * If the setter doesn't exist, try to use the any getter.
             */
            for (Method m : object.getClass().getMethods()) {
                if (m.isAnnotationPresent(JsonAnyGetter.class)) {
                    try {
                        return ((Map)m.invoke(object)).get(key);
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid any getter call: " + key);
                    }
                }
            }

            try {
                return ((Map<String, Object>) object).get(key);
            } catch (ClassCastException ex) {
                return null;
            }
        }
    }

    private Object createChild(Object parent, String key) {
        Map<String, Object> result = Maps.newHashMap();
        try {
            writeMethod(parent, key).invoke(parent, result);
        } catch (Exception e) {

            for (Method m : parent.getClass().getMethods()) {
                if (m.isAnnotationPresent(JsonAnySetter.class)) {
                    try {
                        m.invoke(parent, key, result);
                        // early return
                        return result;
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid attribute: " + key + " parent: " + parent);
                    }
                }
            }

            try {
                ((Map<String, Object>) parent).put(key, result);
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Invalid attribute: " + key + " parent: " + parent);
            }
        }
        return result;
    }

    /**
     * Obtain the read method for the given function name.  Note that, java.beans.PropertyDescriptor
     * does not work when the class has setter functions that return 'this'.  (fluent programming)
     * So the implementation here is less than ideal.
     *
     * @param bean
     * @param name
     * @return
     * @throws NoSuchMethodException
     */
    private static final Method readMethod(Object bean, String name) throws NoSuchMethodException {
        final String capName =StringUtil.capitalize(name);
        for (String methodName: new String[]{
                String.join("", "get", capName),
                String.join("", "is", capName)}) {
            try {
                return bean.getClass().getMethod(methodName);
            } catch (NoSuchMethodException e) {
                continue;
            }
        }
        throw new NoSuchMethodException("Failed to find getter method for '" +
                name + "' on " + bean);
    }

    /**
     * Obtain the write method for the given function name.Note that, java.beans.PropertyDescriptor
     * does not work when the class has setter functions that return 'this'.  (fluent programming)
     * So the implementation here is less than ideal.
     *
     * @param bean
     * @param name
     * @return
     * @throws NoSuchMethodException
     */
    private static final Method writeMethod(Object bean, String name) throws NoSuchMethodException {
        for (Method method: bean.getClass().getMethods()) {
            if (method.getName().equals(String.join("", "set", StringUtil.capitalize(name)))) {
                return method;
            }
        }
        throw new NoSuchMethodException("Failed to find set" + name + " on " + bean.getClass().getName());
    }
}

