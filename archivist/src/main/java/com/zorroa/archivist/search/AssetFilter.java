package com.zorroa.archivist.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Restrict a query to a subset of assets matching the filter.
 *
 * Note: get/is methods that don't return actual properties should always be
 * marked @JsonIgnore.
 */
public class AssetFilter {
    private List<String> exists;
    private List<String> missing;
    private Map<String, List<Object>> terms;
    private Map<String, Map<String,Object>> prefix;
    private Map<String, RangeQuery> range;
    private List<AssetScript> scripts;
    private Map<String, List<Object>> links;
    private Map<String, SimilarityFilter> similarity;
    private Map<String, KwConfFilter> kwconf;
    private Boolean recursive;
    private Map<String, GeoBoundingBox> geo_bounding_box;

    @Deprecated
    private Object hamming;

    /**
     * Hierarchical bool queries combined with implicit "must" filters above
     */
    @JsonProperty("must_not")
    private List<AssetFilter> mustNot;
    private List<AssetFilter> must;
    private List<AssetFilter> should;

    public void merge(AssetFilter filter) {
        if (filter.getLinks() != null) {
            if (links == null) {
                links = new HashMap();
            }
            links.putAll(filter.getLinks());
        }

        if (filter.getExists() != null) {
            if (exists == null) {
                exists = new ArrayList();
            }
            exists.addAll(filter.getExists());
        }

        if (filter.getMissing() != null) {
            if (missing == null) {
                missing = new ArrayList();
            }
            missing.addAll(filter.getMissing());
        }

        if (filter.getTerms() != null) {
            if (terms == null) {
                terms = new HashMap();
                terms.putAll(filter.getTerms());
            }
            else {
                for (Map.Entry<String, List<Object>> entry : filter.getTerms().entrySet()) {
                    List<Object> values = terms.get(entry.getKey());
                    if (values == null) {
                        terms.put(entry.getKey(), new ArrayList(entry.getValue()));
                    }
                    else {
                        values.addAll(entry.getValue());
                    }
                }
            }
        }

        if (filter.getRange() != null) {
            if (range == null) {
                range = new HashMap();
            }
            range.putAll(filter.getRange());
        }

        if (filter.getScripts() != null) {
            if (scripts == null) {
                scripts = new ArrayList();
            }
            scripts.addAll(filter.getScripts());
        }
    }

    public List<String> getExists() {
        return exists;
    }

    public AssetFilter setExists(List<String> exists) {
        this.exists = exists;
        return this;
    }

    public AssetFilter addToExists(String ... field) {
        if (this.exists == null) {
            this.exists = new ArrayList();
        }
        for(String f: field) {
            this.exists.add(f);
        }
        return this;
    }

    public List<String> getMissing() {
        return missing;
    }

    public AssetFilter setMissing(List<String> missing) {
        this.missing = missing;
        return this;
    }

    public AssetFilter addToMissing(String ... field) {
        if (this.missing == null) {
            this.missing = new ArrayList();
        }
        for(String f: field) {
            this.missing.add(f);
        }
        return this;
    }

    public Map<String, List<Object>> getTerms() {
        return terms;
    }

    public AssetFilter setTerms(Map<String, List<Object>> terms) {
        this.terms = terms;
        return this;
    }

    public AssetFilter addToTerms(String field, Object ... value) {
        if (this.terms == null) {
            this.terms = new HashMap();
        }
        if (!this.terms.containsKey(field)) {
            this.terms.put(field, new ArrayList());
        }
        List<Object> _vals = this.terms.get(field);
        for (Object val: value) {
            _vals.add(val);
        }
        return this;
    }

    public AssetFilter addToTerms(String field, List<Object> values) {
        if (this.terms == null) {
            this.terms = new HashMap();
        }
        if (!this.terms.containsKey(field)) {
            this.terms.put(field, new ArrayList());
        }
        List<Object> _vals = this.terms.get(field);
        _vals.addAll(values);
        return this;
    }

    public List<AssetScript> getScripts() {
        return scripts;
    }

    public AssetFilter setScripts(List<AssetScript> scripts) {
        this.scripts = scripts;
        return this;
    }

    public AssetFilter addToScripts(AssetScript script) {
        if (scripts == null) {
            scripts = new ArrayList();
        }
        scripts.add(script);
        return this;
    }

    public Map<String, RangeQuery> getRange() {
        return range;
    }

    public AssetFilter setRange(Map<String, RangeQuery> range) {
        this.range = range;
        return this;
    }

    public AssetFilter addRange(String field, RangeQuery query) {
        if (range == null) {
            range = new HashMap();
        }
        this.range.put(field, query);
        return this;
    }

    public Map<String, List<Object>> getLinks() {
        return links;
    }

    public AssetFilter setLinks(Map<String, List<Object>> links) {
        this.links = links;
        return this;
    }

    public AssetFilter addToLinks(String type, Object ... id) {
        if (links == null) {
            links = new HashMap();
        }
        List<Object> ids = links.get(type);
        if (ids == null) {
            ids = new ArrayList();
            links.put(type, ids);
        }
        for (Object i: id) {
            ids.add(i);
        }
        return this;
    }

    public AssetFilter addToLinks(String type, Collection<Object> ids) {
        return addToLinks(type, ids.stream()
                .map(i->String.valueOf(i)).collect(Collectors.toList()));
    }

    public AssetFilter addToSimilarity(String field, SimilarityFilter filter) {
        if (this.similarity == null) {
            this.similarity = new HashMap();
        }
        this.similarity.put(field, filter);
        return this;
    }

    public Map<String, SimilarityFilter> getSimilarity() {
        return similarity;
    }

    public AssetFilter setSimilarity(Map<String, SimilarityFilter> similarity) {
        this.similarity = similarity;
        return this;
    }

    public List<AssetFilter> getMustNot() {
        return mustNot;
    }

    public AssetFilter setMustNot(List<AssetFilter> mustNot) {
        this.mustNot = mustNot;
        return this;
    }

    public List<AssetFilter> getMust() {
        return must;
    }

    public AssetFilter setMust(List<AssetFilter> must) {
        this.must = must;
        return this;
    }

    public List<AssetFilter> getShould() {
        return should;
    }

    public AssetFilter setShould(List<AssetFilter> should) {
        this.should = should;
        return this;
    }

    public Map<String, Map<String, Object>> getPrefix() {
        return prefix;
    }

    public AssetFilter setPrefix(Map<String, Map<String, Object>> prefix) {
        this.prefix = prefix;
        return this;
    }

    public AssetFilter addToPrefix(String field, Map<String, Object> query) {
        if (prefix == null) {
            prefix = new HashMap();
        }
        prefix.put(field, query);
        return this;
    }

    public Boolean getRecursive() {
        return recursive;
    }

    public AssetFilter setRecursive(Boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    public Object getHamming() {
        return hamming;
    }

    public AssetFilter setHamming(Object hamming) {
        this.hamming = hamming;
        return this;
    }

    public Map<String, KwConfFilter> getKwconf() {
        return kwconf;
    }

    public void setKwconf(Map<String, KwConfFilter> kwconf) {
        this.kwconf = kwconf;
    }

    public AssetFilter addToKwConf(String field, KwConfFilter kwfilt) {
        if (this.kwconf == null) {
            this.kwconf = Maps.newHashMap();
        }
        kwconf.put(field, kwfilt);
        return this;
    }

    public Map<String, GeoBoundingBox> getGeo_bounding_box() {
        return geo_bounding_box;
    }

    public void setGeo_bounding_box(Map<String, GeoBoundingBox> geo_bounding_box) {
        this.geo_bounding_box = geo_bounding_box;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return (links == null ? 0 : links.size()) +
                (exists == null ? 0 : exists.size()) +
                (missing == null ? 0 : missing.size()) +
                (terms == null ? 0 : terms.size()) +
                (range == null ? 0 : range.size()) +
                (scripts == null ? 0 : scripts.size()) +
                (prefix == null ? 0 : prefix.size()) +
                (similarity == null ? 0 : similarity.size()) +
                (kwconf == null ? 0 : kwconf.size()) +
                (geo_bounding_box == null ? 0 : geo_bounding_box.size()) +
                (mustNot == null ? 0 : mustNot.size()) +
                (must == null ? 0 : must.size()) +
                (should == null ? 0 :should.size()) == 0;
    }
}
