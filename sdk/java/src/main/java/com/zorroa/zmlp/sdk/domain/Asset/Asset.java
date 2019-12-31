package com.zorroa.zmlp.sdk.domain.Asset;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The file information and all the metadata generated during Analysis.
 */
public class Asset {

    private String id;
    private Map<String, Object> document;


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
            Map attributes = (Map)f.get("attrs");
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
}
