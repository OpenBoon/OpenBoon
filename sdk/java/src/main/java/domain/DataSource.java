package domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataSource {

    /*
    A DataSource is a remote source for Assets that can be
    iterated by the Analysis framework and imported
    in a single import Job.
     */

    private Map data;

    public DataSource(Map data) {
        this.data = data;
    }

    public String getId() {
        //"""The id of the DataSource"""
        return (String) this.data.get("id");
    }

    public String getName() {
        //"""The name of the DataSource"""
        return (String) this.data.get("name");
    }

    public String getUri() {
        //"""The URI of the DataSource"""
        return (String) this.data.get("uri");
    }

    public List<String> getFileTypes() {
        //"""The file type filter for the DataSource"""
        return (List<String>) Optional.ofNullable(this.data.get("file_types")).orElse(new ArrayList());
    }

    public List getAnalysis() {
        //"""The type of analysis done to the DataSource"""
        return (List) Optional.ofNullable(this.data.get("analysis")).orElse(new ArrayList());

    }
}
