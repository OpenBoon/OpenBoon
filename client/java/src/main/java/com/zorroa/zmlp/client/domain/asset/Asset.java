package com.zorroa.zmlp.client.domain.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.zorroa.zmlp.client.Json;

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

    private static final Pattern ATTR_SPLIT_PATTERN = Pattern.compile(".", Pattern.LITERAL);

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


    /**
     * Return all stored files associated with this asset.  Optionally filter the results.
     *
     * @param assetFilesFilter Filter Description
     * @return
     */

    public List getFiles(AssetFilesFilter assetFilesFilter) {

        // Get Files Object
        List<Map<String, Object>> files = (List) document.getOrDefault("files", new ArrayList());

        // Create Name Filter
        Predicate<Map<String, Object>> namePredicate = f -> {
            String fileNameAttr = (String) f.get("name");
            return fileNameAttr == null ? false : assetFilesFilter.getName().contains(fileNameAttr);
        };

        //Create Category Filter
        Predicate<Map<String, Object>> categoryPredicate = f -> {
            String categoryAttr = (String) f.get("category");
            return assetFilesFilter.getCategory().contains(categoryAttr);
        };

        //Create mimetype Filter
        Predicate<Map<String, Object>> mimeTypePredicate = f ->
                (assetFilesFilter.getMimetype().parallelStream().filter((String mimeType) -> {
                    String mimetypeAttr = (String) f.get("mimetype");
                    return mimetypeAttr == null ? false : mimetypeAttr.startsWith(mimeType);
                }).collect(Collectors.toList()).size() > 0);

        //Create Extension Filter
        Predicate<Map<String, Object>> extensionPredicate = f ->
                (assetFilesFilter.getExtension().parallelStream().filter((String ext) -> {
                    String nameAttr = (String) f.get("name");
                    return nameAttr == null ? false : nameAttr.endsWith(ext);
                })).collect(Collectors.toList()).size() > 0;

        //Create Attrs Filter
        Predicate<Map<String, Object>> attrsPredicate = f ->
                (Boolean) assetFilesFilter.getAttrs().entrySet().stream()
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
            return attributes == null ? false : attributes.keySet().containsAll(assetFilesFilter.getAttrKeys());
        };

        // Check which of predicates will be used
        List<Predicate> elegiblePredicates = new ArrayList();

        //Add Each Existent Predicate
        if (!assetFilesFilter.getName().isEmpty()) elegiblePredicates.add(namePredicate);
        if (!assetFilesFilter.getCategory().isEmpty()) elegiblePredicates.add(categoryPredicate);
        if (!assetFilesFilter.getMimetype().isEmpty()) elegiblePredicates.add(mimeTypePredicate);
        if (!assetFilesFilter.getExtension().isEmpty()) elegiblePredicates.add(extensionPredicate);
        if (!assetFilesFilter.getAttrs().isEmpty()) elegiblePredicates.add(attrsPredicate);
        if (!assetFilesFilter.getAttrKeys().isEmpty()) elegiblePredicates.add(attrsKeysPredicate);

        //Join All predicates
        Predicate compositePredicate = elegiblePredicates.stream().reduce(w -> true, Predicate::and);

        return (List) files.parallelStream().filter(compositePredicate).collect(Collectors.toList());

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
        return (T) getChild(current, attrName(attr));
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
        return Json.mapper.convertValue(getChild(current, attrName(attr)), type);
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
        return Json.mapper.convertValue(getChild(current, attrName(attr)), type);
    }

    /**
     * Return true if the document has the given namespace.
     *
     * @param attr
     * @return
     */
    public boolean attrExists(String attr) {
        Object container = getContainer(attr, false);
        return getChild(container, attrName(attr)) != null;
    }

    /**
     * Set a an attribute value.
     *
     * @param attr  The name of the attr in dot notation.
     * @param value The value of the attr.
     */
    public void setAttr(String attr, Object value) {
        Object current = getContainer(attr, true);
        try {
            String key = attr.substring(attr.lastIndexOf('.') + 1);
            Map<String, Object> map = (Map) current;
            if (value == null) {
                map.put(key, null);
            } else {
                map.put(key, Json.mapper.readValue(Json.asJson(value), Object.class));
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid attribute, " + attr + " is not a map.");
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid attribute, " + attr, e);
        } catch (JsonMappingException e) {
            throw new IllegalArgumentException("Invalid attribute, " + attr + ", cannot be serialized", e);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid attribute, " + attr + ", cannot be serialized", e);
        }
    }

    /**
     * Remove an attribute.
     *
     * @param attr The name of the attr in dot notation.
     * @return True if the attr was removed.
     */
    public boolean removeAttr(String attr) {
        Object current = getContainer(attr, true);
        try {
            String key = attr.substring(attr.lastIndexOf('.') + 1);
            Map<String, Object> map = (Map) current;
            return map.remove(key) != null;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid attribute, " + attr + " is not a map.");
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid attribute, " + attr, e);
        }

    }

    /**
     * Get the parent Map that is storing the given attribute.  If create
     * is true, all parent maps will be made.
     *
     * @param attr   The attribute name in dot notation.
     * @param create If the container should be created.
     * @return
     */
    private Object getContainer(String attr, boolean create) {
        String[] parts = ATTR_SPLIT_PATTERN.split(attr);
        Object current = document;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = getChild(current, parts[i]);
            if (child == null) {
                if (create) {
                    child = createChild(current, parts[i]);
                } else {
                    return null;
                }
            }
            current = child;
        }
        return current;
    }

    /**
     * Get a child value in the given container.
     *
     * @param parent The container, will be cast to a map.
     * @param key    The element name.
     * @return The data in the map or null.
     */
    private Object getChild(Object parent, String key) {
        if (parent == null) {
            return null;
        }
        try {
            Map<String, Object> map = (Map<String, Object>) parent;
            return map.get(key);
        } catch (ClassCastException ex) {
            return null;
        }
    }

    /**
     * Create a child value in the given container.
     *
     * @param parent The container, will be cast to a map.
     * @param key    The element name.
     * @return The data in the map or null.
     */
    private Object createChild(Object parent, String key) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> map = (Map<String, Object>) parent;
            map.put(key, result);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException("Invalid attribute, parent of " + key + " is not a map.");
        }
        return result;
    }

    private String attrName(String attr) {
        return attr.substring(attr.lastIndexOf('.') + 1);
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

    public Asset setDocument(Map<String, Object> document) {
        this.document = document;
        return this;
    }
}
