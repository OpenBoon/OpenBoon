package com.zorroa.common.elastic;


import org.elasticsearch.search.SearchHit;

/**
 * An interface used by ElasticTemplate for mapping documents from an Elastic search result
 * on a per-document basis.  Implementations of this interface perform the actual work of
 * mapping each document to an object.
 *
 * @author chambers
 *
 * @param <T>
 */
public interface SearchHitRowMapper<T> {

     T mapRow(SearchHit hit) throws Exception;

}
