package domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataSource {

    private Map data;

    /**
     * A DataSource is a remote source for Assets that can be
     * iterated by the Analysis framework and imported
     * in a single import Job.
     *
     * @param data Contains DataSource Attributes
     */
    public DataSource(Map data) {
        this.data = data;
    }

    /**
     *
     * @return The id of the DataSource
     */
    public String getId() {
        return (String) this.data.get("id");
    }

    /**
     *
     * @return The name of the DataSource
     */
    public String getName() {
        return (String) this.data.get("name");
    }

    /**
     *
     * @return The URI of the DataSource
     */
    public String getUri() {
        return (String) this.data.get("uri");
    }

    /**
     *
     * @return The file type filter for the DataSource
     */
    public List<String> getFileTypes() {
        return (List<String>) Optional.ofNullable(this.data.get("file_types")).orElse(new ArrayList());
    }

    /**
     *
     * @return The type of analysis done to the DataSource
     */

    public List getAnalysis() {
        return (List) Optional.ofNullable(this.data.get("analysis")).orElse(new ArrayList());

    }
}
