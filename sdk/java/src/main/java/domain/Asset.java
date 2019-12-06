package domain;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Asset extends AssetBase {

    Integer id;

    /*
     """
    An Asset represents a single processed file or a clip/segment of a
    file. Assets start out in the 'CREATED' state, which indicates
    they've been created by not processed.  Once an asset has been processed
    and augmented with files created by various analysis modules, the Asset
    will move into the 'ANALYZED' state.
    """
     */

    public Asset(Map<String, Object> data) throws Exception {

        if (data == null)
            throw new ValueException("Error creating Asset instance, Assets must have an id.");

        this.id = (Integer) data.get("id");
        this.document.getOrDefault("document", new HashMap<>());

    }


    public List getFiles(List<String> name, List<String> category, List<String> mimetype, List<String> extension, Map attrs) {
    /*
            """
        Return all stored files associated with this asset.  Optionally
        filter the results.

        Args:
            name (str): The associated files name.
            category (str): The associated files category, eg proxy, backup, etc.
            mimetype (str): The mimetype must start with this string.
            extension: (str): The file name must have the given extension.
            attrs (dict): The file must have all of the given attributes.

        Returns:
            list of dict: A list of pixml file records.
        """
     */

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
                                    Map attrsObject = (Map)f.get("attrs");
                                    if(attrsObject == null)
                                        return false;

                                    Object o = attrsObject.get(((Map.Entry) entry).getKey());
                                    return o == null ? false : o.equals(key.getValue());
                                }
                        ).reduce((o1, o2) -> ((Boolean) o1) && ((Boolean) o2)).orElse(false);

        // Check which of predicates will be used
        List<Predicate> elegiblePredicates = new ArrayList();
        Optional.ofNullable(name).ifPresent((ignore) -> elegiblePredicates.add(namePredicate));
        Optional.ofNullable(category).ifPresent((ignore) -> elegiblePredicates.add(categoryPredicate));
        Optional.ofNullable(mimetype).ifPresent((ignore) -> elegiblePredicates.add(mimeTypePredicate));
        Optional.ofNullable(extension).ifPresent((ignore) -> elegiblePredicates.add(extensionPredicate));
        Optional.ofNullable(attrs).ifPresent((ignore) -> elegiblePredicates.add(attrsPredicate));

        //Join All predicates
        Predicate compositePredicate = elegiblePredicates.stream().reduce(w -> true, Predicate::and);

        return (List) files.parallelStream().filter(compositePredicate).collect(Collectors.toList());

    }

    public List getFilesByName(String... name) {
        if (name == null)
            return new ArrayList();
        return this.getFiles(Arrays.asList(name), null, null, null, null);
    }

    public List getFilesByCategory(String... category) {
        if (category == null)
            return new ArrayList();
        return this.getFiles(null, Arrays.asList(category), null, null, null);
    }

    public List getFilesByMimetype(String... mimetype) {
        if (mimetype == null)
            return new ArrayList();
        return this.getFiles(null, null, Arrays.asList(mimetype), null, null);
    }

    public List getFilesByExtension(String... extension) {
        if (extension == null)
            return new ArrayList();
        return this.getFiles(null, null, null, Arrays.asList(extension), null);
    }

    public List getFilesByAttrs(Map attrs) {
        if (attrs == null)
            return new ArrayList();
        return this.getFiles(null, null, null, null, attrs);
    }

    @Override
    public Map forJson() {
        /*

        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
                 */
        Map json = new HashMap();
        json.put("id", this.id);
        json.put("document", this.document);
        return json;
    }
}
