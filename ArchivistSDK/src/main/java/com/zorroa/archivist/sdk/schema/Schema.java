package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 11/23/15.
 */
public interface Schema {

    TypeReference<Set<Integer>> SET_OF_INTS = new TypeReference<Set<Integer>>() { };
    TypeReference<Set<String>> SET_OF_STRINGS = new TypeReference<Set<String>>() { };

    TypeReference<List<Integer>> LIST_OF_INTS = new TypeReference<List<Integer>>() { };
    TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() { };

    @JsonIgnore
    String getNamespace();
}
