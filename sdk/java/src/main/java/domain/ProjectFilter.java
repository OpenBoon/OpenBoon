package domain;

import java.util.*;

/**
 * Search filter for finding Projects
 */
public class ProjectFilter {
    /**
     * The Project IDs to match.
     */
    private List<UUID> ids;

    /**
     * The project names to match
     */
    private List<String> names;

    private KPage page;

    private List<String> sort;

    public ProjectFilter(List<UUID> ids, List<String> names, KPage page, List<String> sort) {
        this.ids = ids;
        this.names = names;
        this.page = page;
        this.sort = sort;
    }

    public List<UUID> getIds() {
        return ids;
    }

    public void setIds(List<UUID> ids) {
        this.ids = ids;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public KPage getPage() {
        return page;
    }

    public void setPage(KPage page) {
        this.page = page;
    }

    public List<String> getSort() {
        return sort;
    }

    public void setSort(List<String> sort) {
        this.sort = sort;
    }

    public Map toMap() {

        Map map = new HashMap();

        Optional.ofNullable(ids).ifPresent((List<UUID> value) -> map.put("ids", value));
        Optional.ofNullable(names).ifPresent((List<String> value) -> map.put("names", value));
        Optional.ofNullable(page).ifPresent((KPage value) -> map.put("page", value.toMap()));
        Optional.ofNullable(sort).ifPresent((List<String> value) -> map.put("sort", value));

        return map;
    }
}
