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


    public List getFiles(List<String> name, List<String> category, List<String> mimetype, List<String> extension, List<String> attrs) {
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
        Predicate<Map<String, Object>> namePredicate = f -> name.contains(f.get("name"));

        //Create Category Filter
        Predicate<Map<String, Object>> categoryPredicate = f -> category.contains(f.get("category"));

        //Create mimetype Filter
        Predicate<Map<String, Object>> mimeTypePredicate = f ->
                (mimetype.parallelStream().filter((String mimeType) -> {
                    String fileMimeType = (String) f.get("mimeType");
                    return fileMimeType.startsWith(mimeType);
                }).collect(Collectors.toList()).size() > 0);

        //Create Extension Filter
        Predicate<Map<String, Object>> extensionPredicate = f ->
                (extension.parallelStream().filter((String ext) -> {
                    String str = (String) f.get("name");
                    return str.endsWith(ext);
                })).collect(Collectors.toList()).size() > 0;

        // Check which of going to be used
        List<Predicate> elegiblePredicates = new ArrayList();
        Optional.ofNullable(name).ifPresent((ignore) -> elegiblePredicates.add(namePredicate));
        Optional.ofNullable(category).ifPresent((ignore) -> elegiblePredicates.add(categoryPredicate));
        Optional.ofNullable(mimetype).ifPresent((ignore) -> elegiblePredicates.add(mimeTypePredicate));
        Optional.ofNullable(extension).ifPresent((ignore) -> elegiblePredicates.add(extensionPredicate));

        //Join All predicates
        Predicate compositePredicate = elegiblePredicates.stream().reduce(w -> true, Predicate::and);

        return (List) files.parallelStream().filter(compositePredicate).collect(Collectors.toList());

    }

    public List getFilesByName(String... name) {
        return this.getFiles(Arrays.asList(name), null, null, null, null);
    }

    public List getFilesByCategory(String... category) {
        return this.getFiles(null, Arrays.asList(category), null, null, null);
    }

    public List getFilesByMimetype(String... mimetype) {
        return this.getFiles(null, null, Arrays.asList(mimetype), null, null);
    }

    public List getFilesByExtension(String... extension) {
        return this.getFiles(null, null, null, Arrays.asList(extension), null);
    }

    public List getFilesByAttrs(String... attrs) {
        return this.getFiles(null, null, null, null, Arrays.asList(attrs));
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
