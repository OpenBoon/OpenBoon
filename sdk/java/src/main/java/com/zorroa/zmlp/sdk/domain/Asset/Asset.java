package com.zorroa.zmlp.sdk.domain.Asset;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import com.sun.xml.internal.ws.util.StringUtils;
import com.zorroa.zmlp.sdk.Json;
import com.zorroa.zmlp.sdk.domain.Attr;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * The file information and all the metadata generated during Analysis.
 */
public class Asset {

    private String id;

    private Map<String, Object> document;
    private Map<String, Integer> permissions;
    private boolean replace = false;
    private Double score;
    private List<Asset> elements;
    private String type;
    private String index;

    private static final Logger logger = LoggerFactory.getLogger(Asset.class);
    private static final Pattern PATTERN_ATTR = Pattern.compile(Attr.DELIMITER, Pattern.LITERAL);

    public Asset() {
    }

    public Asset(String id) {
        this.id = id;
        document = new HashMap();
    }

    public Asset(String id, Map<String, Object> document) {
        this.id = id;
        this.document = document;
    }

    public Asset(String id, Map<String, Object> document, Double score, String type, String index) {
        this.id = id;
        this.document = document;
        this.score = score;
        this.type = type;
        this.index = index;
    }

    /**
     * Return all stored files associated with this asset.  Optionally
     * filter the results.
     *
     * @param name      The associated files name.
     * @param category  The associated files category, eg proxy, backup, etc.
     * @param mimetype  The mimetype must start with this string.
     * @param extension The file name must have the given extension.
     * @param attrs     The file must have all of the given attributes.
     * @return List of Dict Pixml file records.
     */

    public List getFiles(List<String> name, List<String> category, List<String> mimetype, List<String> extension, Map attrs, List attrKeys) {

        // Get Files Object
        List<Map<String, Object>> files = (List) document.getOrDefault("files", new ArrayList());

        // Create Name Filter
        Predicate<Map<String, Object>> namePredicate = f -> {
            String fileNameAttr = (String) f.get("name");
            return fileNameAttr == null ? false : name.contains(fileNameAttr);
        };

        //Create Category Filter
        Predicate<Map<String, Object>> categoryPredicate = f -> {
            String categoryAttr = (String) f.get("category");
            return category.contains(categoryAttr);
        };

        //Create mimetype Filter
        Predicate<Map<String, Object>> mimeTypePredicate = f ->
                (mimetype.parallelStream().filter((String mimeType) -> {
                    String mimetypeAttr = (String) f.get("mimetype");
                    return mimetypeAttr == null ? false : mimetypeAttr.startsWith(mimeType);
                }).collect(Collectors.toList()).size() > 0);

        //Create Extension Filter
        Predicate<Map<String, Object>> extensionPredicate = f ->
                (extension.parallelStream().filter((String ext) -> {
                    String nameAttr = (String) f.get("name");
                    return nameAttr == null ? false : nameAttr.endsWith(ext);
                })).collect(Collectors.toList()).size() > 0;

        //Create Attrs Filter
        Predicate<Map<String, Object>> attrsPredicate = f ->
                (Boolean) attrs.entrySet().stream()
                        .map((entry) -> {
                                    Map.Entry key = (Map.Entry) entry;
                                    Map attrsObject = (Map) f.get("attrs");
                                    if (attrsObject == null)
                                        return false;

                                    Object o = attrsObject.get(((Map.Entry) entry).getKey());
                                    return o == null ? false : o.equals(key.getValue());
                                }
                        ).reduce((o1, o2) -> ((Boolean) o1) && ((Boolean) o2)).orElse(false);

        // Create Attrs Keys Filter
        Predicate<Map<String, Object>> attrsKeysPredicate = f -> {
            Map attributes = (Map) f.get("attrs");
            return attributes == null ? false : attributes.keySet().containsAll(attrKeys);
        };

        // Check which of predicates will be used
        List<Predicate> elegiblePredicates = new ArrayList();
        Optional.ofNullable(name).ifPresent((ignore) -> elegiblePredicates.add(namePredicate));
        Optional.ofNullable(category).ifPresent((ignore) -> elegiblePredicates.add(categoryPredicate));
        Optional.ofNullable(mimetype).ifPresent((ignore) -> elegiblePredicates.add(mimeTypePredicate));
        Optional.ofNullable(extension).ifPresent((ignore) -> elegiblePredicates.add(extensionPredicate));
        Optional.ofNullable(attrs).ifPresent((ignore) -> elegiblePredicates.add(attrsPredicate));
        Optional.ofNullable(attrKeys).ifPresent((ignore) -> elegiblePredicates.add(attrsKeysPredicate));

        //Join All predicates
        Predicate compositePredicate = elegiblePredicates.stream().reduce(w -> true, Predicate::and);

        return (List) files.parallelStream().filter(compositePredicate).collect(Collectors.toList());

    }

    /**
     * Return all stored files associated with this asset filtered by name.
     *
     * @param name The associated files name.
     * @return List of Dict Pixml file records.
     */
    public List getFilesByName(String... name) {
        if (name == null)
            return new ArrayList();
        return this.getFiles(Arrays.asList(name), null, null, null, null, null);
    }

    /**
     * Return all stored files associated with this asset filtered by category.
     *
     * @param category The associated files category, eg proxy, backup, etc.
     * @return List of Dict Pixml file records.
     */
    public List getFilesByCategory(String... category) {
        if (category == null)
            return new ArrayList();
        return this.getFiles(null, Arrays.asList(category), null, null, null, null);
    }

    /**
     * Return all stored files associated with this asset filtered by mimetype.
     *
     * @param mimetype The mimetype must start with this string.
     * @return List of Dict Pixml file records.
     */
    public List getFilesByMimetype(String... mimetype) {
        if (mimetype == null)
            return new ArrayList();
        return this.getFiles(null, null, Arrays.asList(mimetype), null, null, null);
    }

    /**
     * Return all stored files associated with this asset filtered by extension.
     *
     * @param extension The file name must have the given extension.
     * @return List of Dict Pixml file records.
     */
    public List getFilesByExtension(String... extension) {
        if (extension == null)
            return new ArrayList();
        return this.getFiles(null, null, null, Arrays.asList(extension), null, null);
    }

    /**
     * Return all stored files associated with this asset filtered by File Attrs.
     *
     * @param attrs The file must have all of the given attributes.
     * @return List of Dict Pixml file records.
     */
    public List getFilesByAttrs(Map attrs) {
        if (attrs == null)
            return new ArrayList();
        return this.getFiles(null, null, null, null, attrs, null);
    }

    /**
     * Return all stored files associated with this asset filtered by by File Attrs Keys.
     *
     * @param attrsKey The file must have all of the given attributes.
     * @return List of Dict Pixml file records.
     */
    public List getFilesByAttrsKey(String... attrsKey) {
        if (attrsKey == null)
            return new ArrayList();
        return this.getFiles(null, null, null, null, null, Arrays.asList(attrsKey));
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
        return Json.mapper.convertValue(getChild(current, Attr.name(attr)), type);
    }

    /**
     * Get an attribute value by its fully qualified name.  Uses a JSON mapper
     * to map the data into the specified TypeReference.
     *
     * @param attr
     * @param type
     * @param <T>
     * @return
     */
    public <T> T getAttr(String attr, TypeReference<T> type) {
        Object current = getContainer(attr, false);
        return Json.mapper.convertValue(getChild(current, Attr.name(attr)), type);
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

        // Look for the addToXXX method.
        try {
            current.getClass().getMethod("addTo" + StringUtils.capitalize(key), Collection.class).invoke(current, value);
            return;
        } catch (Exception ex1) {
            // ignore
        }

        /**
         * Handle the case where the object is a standard map.
         */
        try {
            Map map = ((Map) current);
            Collection collection = (Collection) map.get(key);
            if (collection == null) {
                collection = new ArrayList();
                map.put(key, collection);
            }
            if (value instanceof Collection) {
                collection.addAll((Collection) value);
            } else {
                collection.add(value);
            }
            return;
        } catch (Exception ex2) {
            logger.warn(String.format("The parent attribute %s of type %s is not valid.", attr, current.getClass().getName()));
        }

        /**
         * Handle the JsonAnyGetter case.
         */
        for (Method m : current.getClass().getMethods()) {
            if (m.isAnnotationPresent(JsonAnyGetter.class)) {
                try {
                    Map map = (Map) m.invoke(current);
                    Collection collection = (Collection) map.get(key);
                    if (collection == null) {
                        collection = (Collection) current.getClass().getMethod("getDefaultValue").invoke(current);
                        map.put(key, collection);
                    }

                    if (value instanceof Collection) {
                        collection.addAll((Collection) value);
                    } else {
                        collection.add(value);
                    }
                    return;
                } catch (Exception ex3) {
                    throw new IllegalArgumentException("Invalid any getter call: " + attr + "," + ex3, ex3);
                }
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
            writeMethod(current, key).invoke(current, new Object[]{null});
            return true;
        } catch (Exception e) {
            try {
                return ((Map<String, Object>) current).remove(key) != null;
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Invalid attribute: " + attr);
            }
        }
    }

    /**
     * Return true if the document has the given namespace.
     *
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
        } else if (child instanceof String) {
            return ((String) child).contains(value.toString());
        }
        return false;
    }

    private Object getContainer(String attr, boolean forceExpand) {
        String[] parts = PATTERN_ATTR.split(attr);

        Object current = document;
        for (int i = 0; i < parts.length - 1; i++) {

            Object child = getChild(current, parts[i]);
            if (child == null) {
                if (forceExpand) {
                    child = createChild(current, parts[i]);
                } else {
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
                        return ((Map) m.invoke(object)).get(key);
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
        Map<String, Object> result = new HashMap();
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
        final String capName = StringUtils.capitalize(name);
        for (String methodName : new String[]{
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
        for (Method method : bean.getClass().getMethods()) {
            if (method.getName().equals(String.join("", "set", StringUtils.capitalize(name)))) {
                return method;
            }
        }
        throw new NoSuchMethodException("Failed to find set" + name + " on " + bean.getClass().getName());
    }

    public Map<String, Integer> getPermissions() {
        return permissions;
    }

    public Asset setPermissions(Map<String, Integer> permissions) {
        this.permissions = permissions;
        return this;
    }

    public Asset addToPermissions(String group, int access) {
        if (permissions == null) {
            permissions = new HashMap();
        }
        permissions.put(group, access);
        return this;
    }

    public boolean isReplace() {
        return replace;
    }

    public Asset setReplace(boolean replace) {
        this.replace = replace;
        return this;
    }

    public Double getScore() {
        return score;
    }

    public Asset setScore(Double score) {
        this.score = score;
        return this;
    }

    public List<Asset> getElements() {
        return elements;
    }

    public Asset setElements(List<Asset> elements) {
        this.elements = elements;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset)) return false;
        Asset asset = (Asset) o;
        return id.equals(asset.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getDocument() {
        return document;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

}
