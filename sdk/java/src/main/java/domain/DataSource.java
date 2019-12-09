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

    public Integer getId() {
        //"""The id of the DataSource"""
        return (Integer) this.data.get("id");
    }

    public String getName() {
        //"""The name of the DataSource"""
        return (String) this.data.get("name");
    }

    public String getUri() {
        //"""The URI of the DataSource"""
        return (String) this.data.get("uri");
    }

    public List<String> fileTypes() {
        //"""The file type filter for the DataSource"""
        return (List<String>) Optional.ofNullable(this.data.get("file_types")).orElse(new ArrayList());
    }

    public List analysis() {
        //"""The type of analysis done to the DataSource"""
        return (List) Optional.ofNullable(this.data.get("analysis")).orElse(new ArrayList());

    }



    /*
        def __init__(self, data):
        self._data = data

    @property
    def id(self):
        """The id of the DataSource"""
        return self._data['id']

    @property
    def name(self):
        """The name of the DataSource"""
        return self._data['name']

    @property
    def uri(self):
        """The URI of the DataSource"""
        return self._data['uri']

    @property
    def file_types(self):
        """The file type filter for the DataSource"""
        return self._data.get('file_types', [])

    @property
    def analysis(self):
        """The type of analysis done to the DataSource"""
        return self._data.get('analysis', [])
     */

}
